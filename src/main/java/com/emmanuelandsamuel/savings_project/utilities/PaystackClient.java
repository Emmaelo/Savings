package com.emmanuelandsamuel.savings_project.utilities;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.emmanuelandsamuel.savings_project.dtos.requests.PaystackInitializeRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackInitializeResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackVerifyResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaystackClient {

     private final WebClient webClient;

    @Value("${paystack.secret-key}")
    private String secretKey;


    public PaystackVerifyResponse verifyPayment(String reference) {

        return webClient.get()
            .uri("https://api.paystack.co/transaction/verify/{reference}", reference)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
            .retrieve()
            .bodyToMono(PaystackVerifyResponse.class)
            .doOnError(error -> System.out.println(error.getMessage()))
            .block();
    }


     public PaystackInitializeResponse initializePayment(PaystackInitializeRequest request) {

        return webClient.post()
                .uri("https://api.paystack.co/transaction/initialize")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaystackInitializeResponse.class)
                .block();
    }
}
