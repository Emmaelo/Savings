package com.emmanuelandsamuel.savings_project.services.interfaces;

import com.emmanuelandsamuel.savings_project.dtos.requests.UserRegistrationRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;

public interface UserService {

    ApiResponse<String> registerUser(String idempotencyKey, UserRegistrationRequest userRegistrationRequest);

}
