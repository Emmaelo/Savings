package com.emmanuelandsamuel.savings_project.services.interfaces;

import com.emmanuelandsamuel.savings_project.dtos.requests.UserLoginRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.UserRegistrationRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.LoginResponse;

public interface SignupLoginService {

    // ApiResponse<String> registerUser(String idempotencyKey, UserRegistrationRequest userRegistrationRequest);

    // ApiResponse<LoginResponse> loginUser(String idempotencyKey, UserLoginRequest userLoginRequest);

   ApiResponse <LoginResponse> loginUser2(UserLoginRequest userLoginRequest);
   ApiResponse<String> registerUser2(UserRegistrationRequest request);
}
