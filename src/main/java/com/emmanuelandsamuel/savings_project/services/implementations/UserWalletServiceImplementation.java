package com.emmanuelandsamuel.savings_project.services.implementations;

import com.emmanuelandsamuel.savings_project.dtos.requests.BankAccountRequest;
import com.emmanuelandsamuel.savings_project.entities.User;
import com.emmanuelandsamuel.savings_project.entities.UserBankAccount;
import com.emmanuelandsamuel.savings_project.entities.UserWallet; 
import com.emmanuelandsamuel.savings_project.repositories.UserBankAccountRepositories;
import com.emmanuelandsamuel.savings_project.repositories.UserRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.UserWalletService;
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

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void createUserWallet(String email) {

        try {

            User user = userRepository
                    .findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            UserWallet userWallet = UserWallet.builder().userEmail(email).build();

            user.setUserWallet(userWallet);

            userRepository.save(user);

        } catch (Exception e) {

            log.error("Failed to create wallet for user with email: {}. Error: {}", email, e.getMessage());

            throw e; // Rethrow the exception to trigger transaction rollback

        }
    }

    @Override
    public String addBankAccount(BankAccountRequest request) {
        // String email =
        // SecurityContextHolder.getContext().getAuthentication().getName();

        String email = "emmanuelezeuchegbu@gmail.com";

        List<UserBankAccount> userBankAccount = userBankAccountRepositories.findByUserEmail(email);

        if (userBankAccount.size() >= 3) {
            return "Maxumum number of Account Reached...";
        }

        if (userBankAccountRepositories.existsByUserEmailAndAccountNumberAndBankCode(email, request.getAccountNumber(),
                request.getBankCode())) {
            return " Account already exists";
        }

        UserWallet userWallet = userWalletRepository.findByUserEmail(email).orElse(null);
        if(userWallet.getSecretPin() == null || !passwordEncoder.matches(request.getPin(), userWallet.getSecretPin())){
            return "Invalid Pin";
        }

        userBankAccountRepositories.save(UserBankAccount.builder()
                .accountNumber(request.getAccountNumber())
                .bankCode(request.getBankCode())
                .bankName(request.getBankName())
                .userEmail(email)
                .build());

        return "Account number added...";

    }



    @Override
    public String addSecretPin(String pin) {
        // String email =
        // SecurityContextHolder.getContext().getAuthentication().getName();

        String email = "emmanuel@gmail.com";

       Optional< UserWallet> userWallet = userWalletRepository.findByUserEmail(email);
       if(userWallet.isEmpty()){
        return "User Not Found";
       }
    
        userWallet.get().setSecretPin(passwordEncoder.encode(pin.trim()));
        userWalletRepository.save(userWallet.get());

        return "success";

    }

}
