package com.emmanuelandsamuel.savings_project.services.interfaces;

import com.emmanuelandsamuel.savings_project.dtos.requests.SendEmailRequest;

public interface EmailService {

    void sendEmail(SendEmailRequest sendEmailRequest);

}
