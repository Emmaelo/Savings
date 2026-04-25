package com.emmanuelandsamuel.savings_project.consumers;

import com.emmanuelandsamuel.savings_project.dtos.events.EmailVerificationEvent;
import com.emmanuelandsamuel.savings_project.dtos.requests.SendEmailRequest;
import com.emmanuelandsamuel.savings_project.services.interfaces.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final EmailService emailService;

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = EMAIL_VERIFICATION_KAFKA_TOPIC, groupId = "savings-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendEmailVerificationCode(@Payload Map<String, Object> payload) {


        try {

            EmailVerificationEvent emailVerificationEvent = objectMapper.convertValue(payload, EmailVerificationEvent.class);

            log.info("Received event to send email verification code to {}", emailVerificationEvent.email());

            String messageBody = getVerificationMailBody(emailVerificationEvent.verificationCode());

            SendEmailRequest sendEmailRequest = SendEmailRequest
                    .builder()
                    .recipient(emailVerificationEvent.email())
                    .messageBody(messageBody)
                    .subject("Email Verification")
                    .build();

            emailService.sendEmail(sendEmailRequest);

        } catch (Exception ex) {

            throw new RuntimeException(ex);

        }
    }
}
