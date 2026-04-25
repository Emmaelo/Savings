package com.emmanuelandsamuel.savings_project.dtos.requests;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendEmailRequest {

    private String recipient;

    private String messageBody;

    private String subject;

}
