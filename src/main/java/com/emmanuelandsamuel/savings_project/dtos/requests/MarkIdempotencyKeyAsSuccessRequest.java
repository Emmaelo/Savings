package com.emmanuelandsamuel.savings_project.dtos.requests;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MarkIdempotencyKeyAsSuccessRequest<T> {

    private String idempotencyKey;

    private String eventType;

    private String responseMessage;

    private T responseBody;

}
