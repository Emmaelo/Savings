package com.emmanuelandsamuel.savings_project.services.interfaces;


import com.emmanuelandsamuel.savings_project.dtos.requests.FundWalletRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.PayStackResposeRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.WithdrawalRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.FundWalletResponse;

public interface TransactionService {
    FundWalletResponse initPayment(FundWalletRequest request);
    void savePayStackRefrence(PayStackResposeRequest request);
    String verifyPaystackTransaction(String paystackReference);
    String withdrawFromUserWallet(WithdrawalRequest request);

}
