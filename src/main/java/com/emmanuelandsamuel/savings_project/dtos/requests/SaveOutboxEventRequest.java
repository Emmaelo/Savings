package com.emmanuelandsamuel.savings_project.dtos.requests;

import com.emmanuelandsamuel.savings_project.enumerations.OutboxEventStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaveOutboxEventRequest {

    private UUID id;

    private String eventType;

    private String payload;

    private String kafkaTopic;

    private String idempotencyKey;

    @Builder.Default
    private String status = OutboxEventStatus.PENDING.name();

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    private int maxRetries = 5;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private Long version = 0L;

}
