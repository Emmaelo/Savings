package com.emmanuelandsamuel.savings_project.dtos.requests;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class PaystackWithdrawalRequest {
    private String source;
    private Long amount;
    private String recipient;
    private String reference;
    private String reason;

}
