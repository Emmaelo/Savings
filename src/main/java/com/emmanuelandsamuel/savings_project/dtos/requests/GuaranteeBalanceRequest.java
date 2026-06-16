package com.emmanuelandsamuel.savings_project.dtos.requests;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GuaranteeBalanceRequest {
    private String groupCode;
    private BigDecimal amount;

}
