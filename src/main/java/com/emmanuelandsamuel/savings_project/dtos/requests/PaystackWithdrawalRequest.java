package com.emmanuelandsamuel.savings_project.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaystackWithdrawalRequest {
    private String source;
    private Long amount;
    private String recipient;
    private String reference;
    private String reason;

}
