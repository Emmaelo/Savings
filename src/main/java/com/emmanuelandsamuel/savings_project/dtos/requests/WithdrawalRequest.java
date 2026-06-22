package com.emmanuelandsamuel.savings_project.dtos.requests;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "Pin is required")
    private String pin;

    @NotBlank(message = "Account Number is required")
    @Pattern(regexp = "\\d{10}", message = "Account Number must be exactly 10 digits")
    private String accountNumber;

    @NotBlank(message = "Bank Code required")
    private String bankCode;

    @NotBlank(message = "Bank Name is required")
    private String bankName;

    @NotBlank(message = "Recipient Code is required")
    private String recipientCode;

}
