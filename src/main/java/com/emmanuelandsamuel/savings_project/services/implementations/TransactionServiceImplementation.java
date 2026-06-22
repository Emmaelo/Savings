package com.emmanuelandsamuel.savings_project.services.implementations;

import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.convertKoboToNaira;
import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.convertNairaToKobo;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import com.emmanuelandsamuel.savings_project.dtos.requests.FundWalletRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.PaystackInitializeRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.PaystackWithdrawalRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.WithdrawalRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackInitializeResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackVerifyResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackWithdrawalResponse;
import com.emmanuelandsamuel.savings_project.entities.CompanyWalletLedger;
import com.emmanuelandsamuel.savings_project.entities.OutboxEvent;
import com.emmanuelandsamuel.savings_project.entities.Transactions;
import com.emmanuelandsamuel.savings_project.entities.UserEntity;
import com.emmanuelandsamuel.savings_project.entities.UserWallet;
import com.emmanuelandsamuel.savings_project.entities.UserWalletLedger;
import com.emmanuelandsamuel.savings_project.enumerations.LedgerEntryType;
import com.emmanuelandsamuel.savings_project.enumerations.OutboxEventStatus;
import com.emmanuelandsamuel.savings_project.enumerations.TransactionStatus;
import com.emmanuelandsamuel.savings_project.enumerations.WalletStatus;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import com.emmanuelandsamuel.savings_project.exceptions.WalletNotFoundException;
import com.emmanuelandsamuel.savings_project.repositories.CompanyWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.OutboxEventRepository;
import com.emmanuelandsamuel.savings_project.repositories.TransactionRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserBankAccountRepositories;
import com.emmanuelandsamuel.savings_project.repositories.UserRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.TransactionService;
import com.emmanuelandsamuel.savings_project.utilities.PaystackClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImplementation implements TransactionService {
    private final UserWalletRepository userWalletRepository;
    private final UserWalletLedgerRepository userWalletLedgerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaystackClient paystackClient;
    private final UserBankAccountRepositories userBankAccountRepositories;
    private final TransactionRepository transactionRepository;
    private final CompanyWalletLedgerRepository companyWalletLedgerRepository;
    private static final BigDecimal WITHDRAWAL_FEE = BigDecimal.valueOf(500);
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    // User to make payment into wallet, wallet balance is updated and ledger entry
    // is created for the transaction.
    // An Entity that should hold the trnasaction details before it is sent to the
    // 3rd party payment processor
    // would be ideal here but for simplicity, we will just use the request DTO to
    // hold the transaction details for now.

    @Override
    @Transactional
    public String initPayment(FundWalletRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException("User not found"));

        String transactionReference = generateUniqueTransactionReference();

        // Samuel will run the Idempotency Logic here and return the transaction details
        // if the transaction has already been processed.

        Transactions transaction = Transactions.builder()
                .requestAmount(request.getAmount())
                .transactionReference(transactionReference)
                .userEmail(email)
                .status("PENDING")
                .user(user)
                .build();

        transactionRepository.save(transaction);

        PaystackInitializeResponse paResponse = paystackClient.initializePayment(PaystackInitializeRequest.builder()
                .email(email)
                .amount(convertNairaToKobo(request.getAmount()))
                .currency("NGN")
                .metadata(Map.of(
                        "dbTransactionReference", transactionReference,
                        "firstName", user.getFirstname(),
                        "lastName", user.getLastname(),
                        "currency", "NGN"))
                .build());

        if (paResponse == null || !paResponse.isStatus()) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);

            return "Payment initialization failed. Please try again.";
        }

        savePayStackRefrence(paResponse, transaction);

        return paResponse.getData().getAuthorizationUrl();

    }

    private void savePayStackRefrence(PaystackInitializeResponse paResponse, Transactions transactions) {

        transactions.setPayStackReference(paResponse.getData().getReference());
        transactions.setAuthorizationUrl(paResponse.getData().getAuthorizationUrl());
        transactions.setAccessCode(paResponse.getData().getAccessCode());
        transactions.setTransactionType("CREDIT");
        transactions.setStatus(String.valueOf(TransactionStatus.PROCESSING));
        transactionRepository.save(transactions);
    }

    // Call back from paystack with the transaction reference, verify the
    // transaction and update the transaction status and user wallet accordingly.

    @Transactional
    public String verifyPaystackTransaction(String paystackReference) {

        Transactions transaction = transactionRepository.findByPayStackReferenceForUpdate(paystackReference)
                .orElseThrow(() -> new ApplicationException("Transaction not found"));

        if ("SUCCESS".equals(transaction.getStatus())) {
            return "Transaction has already been verified and wallet credited.";
        }

        PaystackVerifyResponse response = paystackClient.verifyPayment(paystackReference);

        if (!response.isStatus() || !"success".equals(response.getData().getStatus())) {
            transaction.setStatus(response.getData().getStatus().toUpperCase());
            transactionRepository.save(transaction);
            return response != null ? response.getData().getStatus() : "Payment verification failed. Please try again.";
        }

        BigDecimal amountPaid = convertKoboToNaira(response.getData().getAmount());

        if (transaction.getRequestAmount().compareTo(amountPaid) != 0) {
            transaction.setStatus("INCOMPLETE_PAYMENT");
        }

        transaction.setAmountPaid(amountPaid);
        transaction.setPayStackTransactionId(response.getData().getId());
        transaction.setStatus(response.getData().getStatus().toUpperCase());
        transaction.setPaidAt(response.getData().getPaidAt());
        transactionRepository.save(transaction);

        String creditResponse = creditUserWallet(transaction);
        if (creditResponse.equals("success")) {
            return "Payment verified and wallet credited successfully...";
        }

        return " Failed... Wallet Not Credited!!!";
    }

    private String creditUserWallet(Transactions transaction) {
        UserEntity user = transaction.getUser();

        UserWallet wallet = userWalletRepository.findByUserEmailForUpdate(user.getEmail())
                .orElseThrow(() -> new WalletNotFoundException("User wallet not found"));

        BigDecimal amountPaid = transaction.getAmountPaid();

        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amountPaid));
        userWalletRepository.save(wallet);

        userWalletLedgerRepository.save(
                UserWalletLedger.builder()
                        .walletId(wallet.getId())
                        .amount(amountPaid)
                        .email(transaction.getUserEmail())
                        .balanceAfter(wallet.getAvailableBalance())
                        .entryType(LedgerEntryType.CREDIT)
                        .source("PAYSTACK - " + transaction.getPayStackReference())
                        .build());
        return "success";
    }

    @Transactional
    @Override
    public String withdrawFromUserWallet(WithdrawalRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        if (request.getAmount().compareTo(BigDecimal.valueOf(1500)) < 0) {
            return "Minimum withdrawal amount is ₦1000";
        }

        UserWallet userWallet = userWalletRepository.findByUserEmailForUpdate(email)
                .orElseThrow(() -> new WalletNotFoundException("User wallet not found"));

        if (userWallet.getSecretPin() == null
                || !passwordEncoder.matches(request.getPin(), userWallet.getSecretPin())) {
            return "Invalid Pin";
        }

        if (userWallet.getWalletStatus() != WalletStatus.ACTIVE) {
            return "Your wallet is currently not available. Please contact support for assistance.";
        }

        BigDecimal totalDebitAmount = request.getAmount().add(WITHDRAWAL_FEE);

        if (userWallet.getAvailableBalance().compareTo(totalDebitAmount) < 0) {
            return "Insufficient funds...";
        }
        
        if (!userBankAccountRepositories.existsByUserEmailAndAccountNumberAndRecipientCode(email,
                request.getAccountNumber(), request.getRecipientCode())) {
            return " Incorrect Recipient code";
        }


        userWallet.setAvailableBalance(userWallet.getAvailableBalance().subtract(totalDebitAmount));
        userWalletRepository.save(userWallet);

        String reference = generateUniqueTransactionReference();

        userWalletLedgerRepository.save(
                UserWalletLedger.builder()
                        .walletId(userWallet.getId())
                        .amount(request.getAmount())
                        .fee(WITHDRAWAL_FEE)
                        .email(email)
                        .bank(request.getBankName())
                        .transactionReference(reference)
                        .balanceAfter(userWallet.getAvailableBalance())
                        .entryType(LedgerEntryType.DEBIT)
                        .source("USER_WITHDRAWAL")
                        .build());

        companyWalletLedgerRepository.save(
                CompanyWalletLedger.builder()
                        .amount(WITHDRAWAL_FEE)
                        .entryType(LedgerEntryType.CREDIT)
                        .build());

        Transactions transaction = Transactions.builder()
                .requestAmount(totalDebitAmount)
                .status(String.valueOf(TransactionStatus.PROCESSING))
                .bank(request.getBankName())
                .amountPaid(request.getAmount())
                .transactionReference(reference)
                .userEmail(email)
                .transactionType("DEBIT")
                .user(userWallet.getUser())
                .build();
        transactionRepository.save(transaction);

        PaystackWithdrawalRequest paystackrequest = PaystackWithdrawalRequest.builder()
                .amount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .source("balance")
                .recipient(request.getRecipientCode())
                .reference(reference)
                .reason("Withdrawal")
                .build();

        String payload;
        try {
            payload = objectMapper.writeValueAsString(paystackrequest);
            log.info("Payload sent to Kafka: {}", payload);

            OutboxEvent outboxEvent = OutboxEvent.builder()

                    .eventType("WITHDRAWAL")
                    .kafkaTopic("withdrawals")
                    .payload(payload)
                    .status(OutboxEventStatus.PENDING)
                    .build();

            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Error writing value as string : " + e.getMessage());
        }

        return "Processing Withdrawal.";
    }

    private String generateUniqueTransactionReference() {
        return "TXN_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }




    // cron Job

    public PaystackWithdrawalResponse withdrawFromPaystack(PaystackWithdrawalRequest request) {
        return paystackClient.withdrawFromPaystack(request);
    }

  @KafkaListener(topics = "withdrawals")
public void consume(String payload) {

    try {
        log.info("Received payload: {}", payload);

        PaystackWithdrawalRequest request =
                objectMapper.readValue(payload, PaystackWithdrawalRequest.class);

        log.info("Amount: {}", request.getAmount());
        log.info("Reference: {}", request.getReference());

    } catch (Exception e) {
        log.error("Error consuming message", e);
        throw new RuntimeException(e);
    }
}
}
