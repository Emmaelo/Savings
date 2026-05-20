package com.emmanuelandsamuel.savings_project.services.implementations;

import com.emmanuelandsamuel.savings_project.dtos.requests.IdempotencyKeyCheckRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.MarkIdempotencyKeyAsFailedRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.MarkIdempotencyKeyAsSuccessRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.SaveIdempotencyKeyRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.entities.IdempotencyRecord;
import com.emmanuelandsamuel.savings_project.enumerations.IdempotencyStatus;
import com.emmanuelandsamuel.savings_project.repositories.IdempotencyRecordRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.deserialize;
import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.serialize;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImplementation implements IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Override
    public <T> Optional<ApiResponse<T>> checkKey(IdempotencyKeyCheckRequest<T> idempotencyKeyCheckRequest) {

        Optional<IdempotencyRecord> optionalIdempotencyRecord = idempotencyRecordRepository.findByKeyAndEventType(
                idempotencyKeyCheckRequest.getIdempotencyKey(),
                idempotencyKeyCheckRequest.getEventType()
        );

        if (optionalIdempotencyRecord.isEmpty()) {

            log.info(
                    "No existing idempotency record found for key: {} and event type: {}",
                    idempotencyKeyCheckRequest.getIdempotencyKey(),
                    idempotencyKeyCheckRequest.getEventType()
            );

            return Optional.empty();

        }

        IdempotencyRecord existingIdempotencyRecord = optionalIdempotencyRecord.get();

        if (!existingIdempotencyRecord.getRequestFingerprint().equals(idempotencyKeyCheckRequest.getIncomingFingerprint())) {

            log.error(
                    "Idempotency key {} already used for a different request. Existing fingerprint: {}, Incoming fingerprint: {}",
                    idempotencyKeyCheckRequest.getIdempotencyKey(),
                    existingIdempotencyRecord.getRequestFingerprint(),
                    idempotencyKeyCheckRequest.getIncomingFingerprint()
            );

            return Optional.of(ApiResponse.error("Idempotency key already used for a different request"));

        }

        switch (existingIdempotencyRecord.getIdempotencyStatus()) {

            case FAILURE -> {

                return Optional.of(ApiResponse.error(existingIdempotencyRecord.getResponseMessage()));

            }

            case SUCCESS -> {

                return Optional.of(
                        ApiResponse.success(
                                existingIdempotencyRecord.getResponseMessage(),
                                deserialize(existingIdempotencyRecord.getResponseBody(), idempotencyKeyCheckRequest.getResponseType())
                        )
                );

            }

            default -> {

                return Optional.of(ApiResponse.success(existingIdempotencyRecord.getResponseMessage()));

            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public int saveKey(SaveIdempotencyKeyRequest saveIdempotencyKeyRequest) {

        return idempotencyRecordRepository.saveIdempotencyRecordIgnoreConflict(
                saveIdempotencyKeyRequest.getId(),
                saveIdempotencyKeyRequest.getIdempotencyKey(),
                saveIdempotencyKeyRequest.getEventType(),
                saveIdempotencyKeyRequest.getRequestFingerprint(),
                saveIdempotencyKeyRequest.getResponseMessage(),
                saveIdempotencyKeyRequest.getIdempotencyStatus().name(),
                saveIdempotencyKeyRequest.getExpiresAt()
        );

    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public <T> void markKeyAsSuccess(MarkIdempotencyKeyAsSuccessRequest<T> markIdempotencyKeyAsSuccessRequest) {

        idempotencyRecordRepository.findByKeyAndEventType(markIdempotencyKeyAsSuccessRequest.getIdempotencyKey(), markIdempotencyKeyAsSuccessRequest.getEventType())
                .ifPresent(idempotencyRecord -> {

                    idempotencyRecord.setIdempotencyStatus(IdempotencyStatus.SUCCESS);

                    idempotencyRecord.setResponseBody(serialize(markIdempotencyKeyAsSuccessRequest.getResponseBody()));

                    idempotencyRecord.setResponseMessage(markIdempotencyKeyAsSuccessRequest.getResponseMessage());

                    idempotencyRecord.setResolvedAt(LocalDateTime.now());

                    idempotencyRecordRepository.save(idempotencyRecord);
                });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void markKeyAsFailed(MarkIdempotencyKeyAsFailedRequest markIdempotencyKeyAsFailedRequest) {

        idempotencyRecordRepository.findByKeyAndEventType(markIdempotencyKeyAsFailedRequest.getIdempotencyKey(), markIdempotencyKeyAsFailedRequest.getEventType())
                .ifPresent(idempotencyRecord -> {

                    idempotencyRecord.setIdempotencyStatus(IdempotencyStatus.FAILURE);

                    idempotencyRecord.setResolvedAt(LocalDateTime.now());

                    idempotencyRecord.setResponseMessage(markIdempotencyKeyAsFailedRequest.getResponseMessage());

                    idempotencyRecordRepository.save(idempotencyRecord);
                });
    }
}
