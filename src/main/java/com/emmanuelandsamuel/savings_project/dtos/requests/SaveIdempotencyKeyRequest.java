package com.emmanuelandsamuel.savings_project.dtos.requests;

import com.emmanuelandsamuel.savings_project.enumerations.IdempotencyStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaveIdempotencyKeyRequest {

    private UUID id;

    private String idempotencyKey;

    private String eventType;

    private String requestFingerprint;

    private String responseMessage;

    private IdempotencyStatus idempotencyStatus;

    private LocalDateTime expiresAt;
}
