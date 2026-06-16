package com.emmanuelandsamuel.savings_project.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BankAccountRequest {
    private String accountNumber; 
    private String bankCode;
    private String bankName;
    private String pin;
}
