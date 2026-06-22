package com.emmanuelandsamuel.savings_project.dtos.requests;

import lombok.Data;

@Data
public class PaystackRecipientCodeRequest {
    private String type = "nuban";
    private String name;
    private String account_number;
    private String bank_code;
    private String currency = "NGN";

}
