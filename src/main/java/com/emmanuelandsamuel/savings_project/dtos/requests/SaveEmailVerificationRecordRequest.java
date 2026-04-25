package com.emmanuelandsamuel.savings_project.dtos.requests;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaveEmailVerificationRecordRequest {

    private UUID id;

    private String email;

    private String hashedVerificationCode;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15L);

    @Builder.Default
    private Long version = 0L;

    @Builder.Default
    private boolean isVerified = false;
}
