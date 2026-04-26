package com.emmanuelandsamuel.savings_project.services.implementations;

import com.emmanuelandsamuel.savings_project.dtos.requests.SaveOutboxEventRequest;
import com.emmanuelandsamuel.savings_project.repositories.OutboxEventRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventServiceImplementation implements OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void saveOutboxEvent(SaveOutboxEventRequest saveOutboxEventRequest) {

        try {

            int insertedRows = outboxEventRepository.insertIgnoreConflict(
                    saveOutboxEventRequest.getId(),
                    saveOutboxEventRequest.getEventType(),
                    saveOutboxEventRequest.getPayload(),
                    saveOutboxEventRequest.getKafkaTopic(),
                    saveOutboxEventRequest.getIdempotencyKey(),
                    saveOutboxEventRequest.getStatus(),
                    saveOutboxEventRequest.getRetryCount(),
                    saveOutboxEventRequest.getMaxRetries(),
                    saveOutboxEventRequest.getCreatedAt(),
                    saveOutboxEventRequest.getVersion()
            );

            if (insertedRows == 0)
                log.warn(
                        "Duplicate outbox event detected for idempotencyKey={} and eventType={}. Skipping creation.",
                        saveOutboxEventRequest.getIdempotencyKey(),
                        saveOutboxEventRequest.getEventType()
                );

        } catch (Exception ex) {

            log.error(
                    "Error occurred while saving outbox event with idempotencyKey={} and eventType={}. Exception: {}",
                    saveOutboxEventRequest.getIdempotencyKey(),
                    saveOutboxEventRequest.getEventType(),
                    ex.getMessage()
            );

            throw ex; // Rethrow the exception to ensure transaction rollback

        }
    }
}
