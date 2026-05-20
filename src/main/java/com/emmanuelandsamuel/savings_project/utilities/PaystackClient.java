package com.emmanuelandsamuel.savings_project.utilities;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackVerifyResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaystackClient {

     private WebClient webClient;

    @Value("${paystack.secret-key}")
    private String secretKey;


    public PaystackVerifyResponse verifyPayment(String reference) {

        return webClient.get()
            .uri("https://api.paystack.co/transaction/verify/{reference}", reference)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
            .retrieve()
            .bodyToMono(PaystackVerifyResponse.class)
            .block();
    }
}
