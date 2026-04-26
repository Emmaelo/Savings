package com.emmanuelandsamuel.savings_project.dtos.requests;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MarkIdempotencyKeyAsFailedRequest {

    String idempotencyKey;

    String eventType;

    String responseMessage;

}
