package com.emmanuelandsamuel.savings_project.utilities;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.emmanuelandsamuel.savings_project.dtos.requests.PaystackInitializeRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackAccountResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackBankResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackInitializeResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackVerifyResponse;
import com.emmanuelandsamuel.savings_project.entities.Banks;
import com.emmanuelandsamuel.savings_project.repositories.BankRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaystackClient {

     private final WebClient webClient;
     private final BankRepository bankRepository;

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


    
    public PaystackAccountResponse resolveAccount(String accountNumber, String bankCode) {

        return webClient.get()
                .uri("https://api.paystack.co/bank/resolve?account_number={acc}&bank_code={bank}",
                        accountNumber, bankCode)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Paystack error: " + body))
                )
                .bodyToMono(PaystackAccountResponse.class)
                .block();
    }




    // To be Schedulled to run once every month
  public void syncBanksFromPaystack() {

        PaystackBankResponse response = webClient.get()
                .uri("https://api.paystack.co/bank")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .retrieve()
                .bodyToMono(PaystackBankResponse.class)
                .block();

        if (response == null || !response.isStatus()) {
            return;
        }

        Map<String, PaystackBankResponse.BankData> uniqueBanks =
        response.getData().stream()
                .filter(PaystackBankResponse.BankData::isActive)
                .collect(Collectors.toMap(
                        PaystackBankResponse.BankData::getCode,
                        Function.identity(),
                        (first, second) -> first
                ));

                List<Banks> banksToSave = uniqueBanks.values().stream()
        .filter(bank -> !bankRepository.existsByCode(bank.getCode()))
        .map(bankData -> {
            Banks bank = new Banks();
            bank.setName(bankData.getName());
            bank.setCode(bankData.getCode());
            bank.setSlug(bankData.getSlug());
            bank.setActive(bankData.isActive());
            return bank;
        })
        .toList();

        bankRepository.saveAll(banksToSave);
    }




}
