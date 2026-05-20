package com.emmanuelandsamuel.savings_project.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PaystackVerifyResponse {

    private boolean status;

    private String message;

    private PaystackData data;



@Data
public static class PaystackData {
    private Long id;
    private String reference;
    private Long amount;
    private String status;
     private String currency;
    @JsonProperty("paid_at")
    private String paidAt;
   
}

}