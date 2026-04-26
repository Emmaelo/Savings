package com.emmanuelandsamuel.savings_project.services.interfaces;

import com.emmanuelandsamuel.savings_project.dtos.requests.IdempotencyKeyCheckRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.MarkIdempotencyKeyAsFailedRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.MarkIdempotencyKeyAsSuccessRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.SaveIdempotencyKeyRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;

import java.util.Optional;

public interface IdempotencyService {

    <T> Optional<ApiResponse<T>> checkKey(IdempotencyKeyCheckRequest<T> idempotencyKeyCheckRequest);

    int saveKey(SaveIdempotencyKeyRequest saveIdempotencyKeyRequest);

    <T> void markKeyAsSuccess(MarkIdempotencyKeyAsSuccessRequest<T> markIdempotencyKeyAsSuccessRequest);

    void markKeyAsFailed(MarkIdempotencyKeyAsFailedRequest markIdempotencyKeyAsFailedRequest);

}
