package com.emmanuelandsamuel.savings_project.dtos.requests;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaystackInitializeRequest {
    private String email;
    private Long amount;
    private String currency;
    private Map<String, Object> metadata;


}
