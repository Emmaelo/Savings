package com.emmanuelandsamuel.savings_project.services.implementations;

import com.emmanuelandsamuel.savings_project.entities.User;
import com.emmanuelandsamuel.savings_project.entities.UserWallet;
import com.emmanuelandsamuel.savings_project.repositories.UserRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.UserWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserWalletServiceImplementation implements UserWalletService {

    private final UserRepository userRepository;

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
}
