
package com.emmanuelandsamuel.savings_project.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BankListResponse {
    private String accountNumber;
    private String bankCode;
    private String bankName;

}
