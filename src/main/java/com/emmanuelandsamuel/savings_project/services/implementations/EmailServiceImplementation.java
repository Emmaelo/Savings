package com.emmanuelandsamuel.savings_project.services.implementations;

import com.emmanuelandsamuel.savings_project.dtos.requests.SendEmailRequest;
import com.emmanuelandsamuel.savings_project.services.interfaces.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImplementation implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${email.username}")
    private String emailSender;

    @Override
    public void sendEmail(SendEmailRequest sendEmailRequest) {

        try {

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

            mimeMessageHelper.setFrom(emailSender);

            mimeMessageHelper.setTo(sendEmailRequest.getRecipient());

            mimeMessageHelper.setText(sendEmailRequest.getMessageBody(), true);

            mimeMessageHelper.setSubject(sendEmailRequest.getSubject());

            javaMailSender.send(mimeMessage);

        } catch (Exception ex) {

            log.error("Error sending email to: {} with exception {}", sendEmailRequest.getRecipient(), ex.getMessage(), ex);

            throw new RuntimeException("Failed to send email to: " + sendEmailRequest.getRecipient(), ex);

        }
    }
}
