package com.emmanuelandsamuel.savings_project.dtos.requests;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WithdrawalRequest {
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String pin;

    private String accountNumber;
    
    private String bankCode;

    private String bankName;

    private String recipientCode;

}
