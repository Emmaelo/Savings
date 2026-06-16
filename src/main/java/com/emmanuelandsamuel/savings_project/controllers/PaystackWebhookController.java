package com.emmanuelandsamuel.savings_project.controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;

import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.PaystackAccountResponse;
import com.emmanuelandsamuel.savings_project.entities.Banks;
import com.emmanuelandsamuel.savings_project.repositories.BankRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.TransactionService;
import com.emmanuelandsamuel.savings_project.utilities.PaystackClient;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments/paystack")
@RequiredArgsConstructor
public class PaystackWebhookController {



    private final PaystackClient paystackClient;
    private final BankRepository bankRepository;


     @Operation(summary = "Resolve Account")
    @GetMapping("/{accountNumber}/{bankCode}")
    public  ResponseEntity<ApiResponse<PaystackAccountResponse>> resolveAccount(@PathVariable String accountNumber, @PathVariable String bankCode){
        return ResponseEntity.ok().body(new ApiResponse<>(true, "success", paystackClient.resolveAccount(accountNumber, bankCode)));

    }


    @Operation(summary = "Get bank list")
      @GetMapping("/")
    public  ResponseEntity <ApiResponse<List<Banks>>> getBankList(){
        return ResponseEntity.ok().body(new ApiResponse<>(true, "success", bankRepository.findAll()));

    }






    

    // private final TransactionService transactionService;
    // private final ObjectMapper objectMapper;

    // @Value("${paystack.webhook-secret}")
    // private String paystackWebhookSecret;

    // @PostMapping("/webhook")
    // public ResponseEntity<Void> handleWebhook(
    //         @RequestHeader(value = "X-Paystack-Signature", required = false) String signature,
    //         @RequestBody String payload
    // ) {
    //     if (signature == null || signature.isBlank()) {
    //         return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    //     }

    //     if (!isValidSignature(payload, signature, paystackWebhookSecret)) {
    //         return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    //     }

    //     try {
    //         // PaystackWebhookRequest webhookRequest = objectMapper.readValue(payload, PaystackWebhookRequest.class);
    //         // transactionService.handlePaystackWebhook(webhookRequest);
    //         return ResponseEntity.ok().build();
    //     } catch (Exception ex) {
    //         return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    //     }

    // }

    // private boolean isValidSignature(String payload, String signature, String secret) {
    //     try {
    //         Mac mac = Mac.getInstance("HmacSHA512");
    //         mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
    //         byte[] expectedBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    //         StringBuilder expectedHex = new StringBuilder();
    //         for (byte b : expectedBytes) {
    //             expectedHex.append(String.format("%02x", b));
    //         }
    //         return expectedHex.toString().equalsIgnoreCase(signature);
    //     } catch (Exception e) {
    //         return false;
    //     }
    // }
}
