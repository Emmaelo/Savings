package com.emmanuelandsamuel.savings_project.services.interfaces;

import com.emmanuelandsamuel.savings_project.dtos.requests.EmailVerificationRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;

public interface EmailVerificationService {

    ApiResponse<String> sendVerificationEmail(String idempotencyKey, String email);

    ApiResponse<String> verifyVerificationCode(EmailVerificationRequest emailVerificationRequest);

    boolean isEmailVerified(String email);

}
