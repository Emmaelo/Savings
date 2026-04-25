package com.emmanuelandsamuel.savings_project.consumers;

import com.emmanuelandsamuel.savings_project.dtos.events.EmailVerificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.EMAIL_VERIFICATION_KAFKA_TOPIC;
import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.serialize;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaDlqConsumerService {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = EMAIL_VERIFICATION_KAFKA_TOPIC + ".DLT", groupId = "savings-group-dlq", containerFactory = "dltKafkaListenerContainerFactory")
    public void sendEmailVerificationCodeDlq(@Payload Map<String, Object> payload) {

        EmailVerificationEvent emailVerificationEvent = objectMapper.convertValue(payload, EmailVerificationEvent.class);

        log.info("Received event for sending email verification in DLT: {}", serialize(emailVerificationEvent));

    }
}
