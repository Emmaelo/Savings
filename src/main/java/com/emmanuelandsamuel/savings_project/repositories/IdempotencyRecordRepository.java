package com.emmanuelandsamuel.savings_project.repositories;

import com.emmanuelandsamuel.savings_project.entities.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByKeyAndEventType(String key, String eventType);

    void deleteAllByExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Query(value = """
              INSERT INTO idempotency_records (
                id,
                key,
                event_type,
                request_fingerprint,
                response_message,
                idempotency_status,
                created_at,
                expires_at
            )
            VALUES (
                :id,
                :idempotencyKey,
                :eventType,
                :requestFingerPrint,
                :responseMessage,
                :idempotencyStatus,
                now(),
                :expiresAt
            )
            ON CONFLICT (key, event_type) DO NOTHING
            """, nativeQuery = true)
    int saveIdempotencyRecordIgnoreConflict(
            @Param("id") UUID id,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("eventType") String eventType,
            @Param("requestFingerPrint") String requestFingerPrint,
            @Param("responseMessage") String responseMessage,
            @Param("idempotencyStatus") String idempotencyStatus,
            @Param("expiresAt") LocalDateTime expiresAt
    );

}
