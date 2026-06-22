package com.emmanuelandsamuel.savings_project.dtos.responses;

import lombok.Data;

@Data
public class PaystackRecipientCodeResponse {
    private boolean status;
    private String message;
    private Data data;

    @lombok.Data
    public static class Data {
        private String recipient_code;
    }

}
