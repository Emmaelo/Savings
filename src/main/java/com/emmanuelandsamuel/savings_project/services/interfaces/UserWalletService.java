package com.emmanuelandsamuel.savings_project.services.interfaces;

import com.emmanuelandsamuel.savings_project.dtos.requests.BankAccountRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public interface UserWalletService {

    void createUserWallet(String email);
    String addBankAccount(BankAccountRequest bankAccountRequest);
    String addSecretPin(@NotBlank(message = "Cannot be empty") @Pattern(regexp = "\\d{4}", message = "Pin must be 4 digits") String pin);

}
