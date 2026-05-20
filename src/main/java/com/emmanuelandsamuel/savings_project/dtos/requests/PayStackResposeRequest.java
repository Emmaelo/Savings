package com.emmanuelandsamuel.savings_project.dtos.requests;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayStackResposeRequest {

    private String authorizationUrl;
    private String accessCode;
    private String payStackReference;
    private String transactionReference;
}
