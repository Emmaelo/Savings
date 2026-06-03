package com.emmanuelandsamuel.savings_project.controllers;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.emmanuelandsamuel.savings_project.dtos.requests.FundWalletRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.PayStackResposeRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.WithdrawalRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.FundWalletResponse;
import com.emmanuelandsamuel.savings_project.services.interfaces.TransactionService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "APIs for transaction management")
public class TransactionController {
    
    private final TransactionService transactionService;


    @Operation(summary = "Initialize payment")
    @PostMapping("/initialize-payment")
    public  FundWalletResponse initPayment(@RequestBody FundWalletRequest request) {
        return transactionService.initPayment(request);
    }

    @Operation(summary = "Verify Paystack transaction")
    @PostMapping("/verify-transaction/{paystackReference}")
     public String verifyPaystackTransaction(@PathVariable String paystackReference) {
        return transactionService.verifyPaystackTransaction(paystackReference);
    }

    @Operation(summary = "Withdraw from user wallet")
    @PostMapping("/withdraw")
     public String withdrawFromUserWallet(@RequestBody WithdrawalRequest request) {
        return transactionService.withdrawFromUserWallet(request);
    }

    @Operation(summary = "Save Paystack reference")
    @PostMapping("/save-paystack-reference")
    public void savePayStackRefrence(@RequestBody PayStackResposeRequest request) {
        transactionService.savePayStackRefrence(request);
    }
}
