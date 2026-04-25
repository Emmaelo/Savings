package com.emmanuelandsamuel.savings_project.dtos.requests;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IdempotencyKeyCheckRequest<T> {

    private String idempotencyKey;

    private String eventType;

    private String incomingFingerprint;

    private Class<T> responseType;

}
