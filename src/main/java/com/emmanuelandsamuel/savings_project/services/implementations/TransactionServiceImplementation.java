package com.emmanuelandsamuel.savings_project.services.implementations;

import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.convertKoboToNaira;
import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.convertNairaToKobo;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import com.emmanuelandsamuel.savings_project.dtos.requests.FundWalletRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.PayStackResposeRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.WithdrawalRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.FundWalletResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackVerifyResponse;
import com.emmanuelandsamuel.savings_project.entities.CompanyWalletLedger;
import com.emmanuelandsamuel.savings_project.entities.Transactions;
import com.emmanuelandsamuel.savings_project.entities.User;
import com.emmanuelandsamuel.savings_project.entities.UserWallet;
import com.emmanuelandsamuel.savings_project.entities.UserWalletLedger;
import com.emmanuelandsamuel.savings_project.enumerations.LedgerEntryType;
import com.emmanuelandsamuel.savings_project.enumerations.WalletStatus;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import com.emmanuelandsamuel.savings_project.exceptions.WalletNotFoundException;
import com.emmanuelandsamuel.savings_project.repositories.CompanyWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.TransactionRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.TransactionService;
import com.emmanuelandsamuel.savings_project.utilities.PaystackClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionServiceImplementation implements TransactionService {
    private final UserWalletRepository userWalletRepository;
    private final UserWalletLedgerRepository userWalletLedgerRepository;
    private final UserRepository userRepository;
    private final PaystackClient paystackClient;
    private final TransactionRepository transactionRepository;
    private final CompanyWalletLedgerRepository companyWalletLedgerRepository;
    private static final BigDecimal WITHDRAWAL_FEE = BigDecimal.valueOf(1000);


    //User to make payment into wallet, wallet balance is updated and ledger entry is created for the transaction. 
    //An Entity that should hold the trnasaction details before it is sent to the 3rd party payment processor 
    // would be ideal here but for simplicity, we will just use the request DTO to hold the transaction details for now.

    @Override
    @Transactional
    public FundWalletResponse initPayment(FundWalletRequest request) {
        // String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = "emmanuelezeuchegbu@gmail.com";

        User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ApplicationException("User not found"));

         String transactionReference = generateUniqueTransactionReference();

        // Samuel will run the Idempotency Logic here and return the transaction details if the transaction has already been processed.

        Transactions transaction = Transactions.builder()
                .amount(request.getAmount())
                .transactionReference(transactionReference)
                .status("PENDING")
                .bank(request.getBank())
                .user(user)
                .build();

            transactionRepository.save(transaction);
        
       

        return FundWalletResponse.builder()
        .amount(convertNairaToKobo(request.getAmount()))
        .email(email)
        .metadata(Map.of(
        "dbTransactionReference", transactionReference, 
        "firstName", user.getFirstname(),
        "lastName", user.getLastname(),
        "currency", "KOBO"
        ))
        .build();

    }



    @Transactional
    @Override
    public void savePayStackRefrence(PayStackResposeRequest request) {
        Transactions transaction = transactionRepository.findByTransactionReference(request.getTransactionReference())
                .orElseThrow(() -> new ApplicationException("Transaction not found"));

        transaction.setPayStackReference(request.getPayStackReference());
        transaction.setAuthorizationUrl(request.getAuthorizationUrl());
        transaction.setAccessCode(request.getAccessCode());
        transaction.setStatus("PROCESSING");
        transactionRepository.save(transaction);
    }



    @Transactional
    public String verifyPaystackTransaction(String paystackReference) {

        Transactions transaction = transactionRepository.findByPayStackReference(paystackReference)
                .orElseThrow(() -> new ApplicationException("Transaction not found"));

            PaystackVerifyResponse response = paystackClient.verifyPayment(paystackReference);

    if (!response.isStatus() || !"success".equals(response.getData().getStatus())) {
        transaction.setStatus(response.getData().getStatus().toUpperCase());
        transactionRepository.save(transaction);
        return "Payment verification failed...";
    }

    BigDecimal amountPaid = convertKoboToNaira(response.getData().getAmount());


       if (transaction.getAmount().compareTo(amountPaid) != 0) {
        transaction.setStatus("INCOMPLETE_PAYMENT");
        transactionRepository.save(transaction);
        throw new ApplicationException("Payment amount mismatch");
    }


    transaction.setAmountPaid(amountPaid);
    transaction.setStatus(response.getData().getStatus().toUpperCase());
    transactionRepository.save(transaction);
    transaction.setPaidAt(response.getData().getPaidAt());

    creditUserWallet(transaction);

    return "Payment verified and wallet credited successfully...";
    }



    private void creditUserWallet(Transactions transaction) {
        User user = transaction.getUser();
        UserWallet wallet = user.getUserWallet();
        BigDecimal amount = transaction.getAmount();

        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        userWalletRepository.save(wallet);

        userWalletLedgerRepository.save(
                UserWalletLedger.builder()
                        .walletId(wallet.getId())
                        .amount(amount)
                        .balanceAfter(wallet.getAvailableBalance())
                        .entryType(LedgerEntryType.CREDIT)
                        .source("PAYSTACK - "+ transaction.getPayStackReference())
                        .build()
        );
    }


    @Transactional
    @Override
    public String withdrawFromUserWallet(WithdrawalRequest request) {
        // String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = "emmanuelezeuchegbu@gmail.com";

        UserWallet userWallet = userWalletRepository.findByUserEmail(email)
        .orElseThrow(() -> new WalletNotFoundException("User wallet not found"));

        if(!userWallet.getWalletStatus().equals(WalletStatus.ACTIVE)) {
            return "Your wallet is currently not available. Please contact support for assistance.";
        }
        BigDecimal totalDebitAmount = request.getAmount().add(WITHDRAWAL_FEE);

        if (userWallet.getAvailableBalance().compareTo(totalDebitAmount) < 0) {
            return "Insufficient funds...";
        }

        
        userWallet.setAvailableBalance(userWallet.getAvailableBalance().subtract(totalDebitAmount));
        userWalletRepository.save(userWallet); 

        //CREDIT THE COMPANY WALLET WITH THE FEE AND DEBIT THE USER WALLET WITH THE AMOUNT + FEE

            userWalletLedgerRepository.save(
         UserWalletLedger.builder()
                 .walletId(userWallet.getId())
                 .amount(request.getAmount())
                 .fee(WITHDRAWAL_FEE)
                 .balanceAfter(userWallet.getAvailableBalance())
                 .entryType(LedgerEntryType.DEBIT)
                 .source("USER_WITHDRAWAL")
                 .build()
            );

            companyWalletLedgerRepository.save(
                    CompanyWalletLedger.builder()
                        .amount(WITHDRAWAL_FEE)
                        .entryType(LedgerEntryType.CREDIT)
                        .build()
                );
        
            //Kafka and actual withdrawal 3rd party logic would go here..
        return "Withdraw Successful.";
    }


    
    
    private String generateUniqueTransactionReference() {
    return "TXN_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
  }


}
