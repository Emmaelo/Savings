package com.emmanuelandsamuel.savings_project.dtos.responses;

import lombok.Data;

@Data
public class PaystackAccountResponse {
    private boolean status;
    private String message;
    private Data data;

    
    @lombok.Data
    public static class Data {
        private String account_number;
        private String account_name;
        private Long bank_id;
    }
}

