package com.emmanuelandsamuel.savings_project.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ApiResponse<T> {

    private String responseMessage;

    private T responseBody;

    private boolean isSuccessful;

    private String responseTime;

    public ApiResponse(boolean isSuccessful, String responseMessage) {

        this.isSuccessful = isSuccessful;

        this.responseMessage = responseMessage;

        this.responseTime = LocalDateTime.now().toString();

        this.responseBody = null;

    }

    public ApiResponse(boolean isSuccessful, String responseMessage, T responseBody) {

        this.responseMessage = responseMessage;

        this.isSuccessful = isSuccessful;

        this.responseBody = responseBody;

        this.responseTime = LocalDateTime.now().toString();

    }

    public static <T> ApiResponse<T> success(String message, T data) {

        return new ApiResponse<>(true, message, data);

    }

    public static <T> ApiResponse<T> success(String message) {

        return new ApiResponse<>(true, message);

    }

    public static <T> ApiResponse<T> error(String message) {

        return new ApiResponse<>(false, message);

    }

    public static <T> ApiResponse<T> validationError(String message, T validationErrors) {

        return new ApiResponse<>(false, message, validationErrors);

    }
}