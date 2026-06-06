package com.emmanuelandsamuel.savings_project.services.interfaces;


import com.emmanuelandsamuel.savings_project.dtos.requests.FundWalletRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.WithdrawalRequest;

public interface TransactionService {
    String initPayment(FundWalletRequest request);
    String verifyPaystackTransaction(String paystackReference);
    String withdrawFromUserWallet(WithdrawalRequest request);

}
