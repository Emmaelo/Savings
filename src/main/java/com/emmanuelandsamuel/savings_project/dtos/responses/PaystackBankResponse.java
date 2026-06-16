package com.emmanuelandsamuel.savings_project.dtos.responses;

import java.util.List;
import lombok.Data;

@Data
public class PaystackBankResponse {
    private boolean status;
    private String message;
    private List<BankData> data;

    @Data
    public static class BankData {
        private String name;
        private String code;
        private String slug;
        private boolean active;
    }
}
