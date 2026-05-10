package com.emmanuelandsamuel.savings_project.services.interfaces;

import com.emmanuelandsamuel.savings_project.dtos.requests.UserLoginRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.UserRegistrationRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.LoginResponse;

public interface UserService {

    ApiResponse<String> registerUser(String idempotencyKey, UserRegistrationRequest userRegistrationRequest);

    ApiResponse<LoginResponse> loginUser(String idempotencyKey, UserLoginRequest userLoginRequest);

}
