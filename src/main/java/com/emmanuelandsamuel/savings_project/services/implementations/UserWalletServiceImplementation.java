package com.emmanuelandsamuel.savings_project.services.implementations;

import com.emmanuelandsamuel.savings_project.dtos.requests.BankAccountRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.PaystackRecipientCodeRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackRecipientCodeResponse;
import com.emmanuelandsamuel.savings_project.entities.UserEntity;
import com.emmanuelandsamuel.savings_project.entities.UserBankAccount;
import com.emmanuelandsamuel.savings_project.entities.UserWallet;
import com.emmanuelandsamuel.savings_project.repositories.UserBankAccountRepositories;
import com.emmanuelandsamuel.savings_project.repositories.UserRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.UserWalletService;
import com.emmanuelandsamuel.savings_project.utilities.PaystackClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserWalletServiceImplementation implements UserWalletService {

    private final UserRepository userRepository;
    private final UserBankAccountRepositories userBankAccountRepositories;
    private final UserWalletRepository userWalletRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaystackClient paystackClient;

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void createUserWallet(String email, String phoneNumber) {

        try {

            UserEntity user = userRepository
                    .findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            UserWallet userWallet = UserWallet.builder()
            .userEmail(email)
            .build();

            user.setUserWallet(userWallet);

            userRepository.save(user);

        } catch (Exception e) {

            log.error("Failed to create wallet for user with email: {}. Error: {}", email, e.getMessage());

            throw e; // Rethrow the exception to trigger transaction rollback

        }
    }

    @Override
    public String addBankAccount(BankAccountRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        List<UserBankAccount> userBankAccount = userBankAccountRepositories.findByUserEmail(email);

        if (userBankAccount.size() >= 3) {
            return "Maxumum number of Account Reached...";
        }

        if (userBankAccountRepositories.existsByUserEmailAndAccountNumberAndBankCode(email, request.getAccountNumber(),
                request.getBankCode())) {
            return " Account already exists";
        }

        PaystackRecipientCodeRequest recipientCodeRequest = new PaystackRecipientCodeRequest();
        recipientCodeRequest.setAccount_number(request.getAccountNumber());
        recipientCodeRequest.setName(request.getName());
        recipientCodeRequest.setBank_code(request.getBankCode());

        PaystackRecipientCodeResponse response = paystackClient.creaRecipientCode(recipientCodeRequest);

        if (response == null || !response.isStatus()) {
            return "Unable to create recipient code";
        }

        userBankAccountRepositories.save(UserBankAccount.builder()
                .accountNumber(request.getAccountNumber())
                .bankCode(request.getBankCode())
                .bankName(request.getBankName())
                .userEmail(email)
                .recipientCode(response.getData().getRecipient_code())
                .build());

        return "Account number added...";

    }


    // OTP would be sent to users Email before adding pin to ensure maximum security..
    @Override
    public String addSecretPin(String pin) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<UserWallet> userWallet = userWalletRepository.findByUserEmail(email);
        if (userWallet.isEmpty()) {
            return "User Not Found";
        }

        userWallet.get().setSecretPin(passwordEncoder.encode(pin.trim()));
        userWalletRepository.save(userWallet.get());

        return "success";

    }



}
