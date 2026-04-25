package com.emmanuelandsamuel.savings_project.repositories;

import com.emmanuelandsamuel.savings_project.entities.OutboxEvent;
import com.emmanuelandsamuel.savings_project.enumerations.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO outbox_events (
                id,
                event_type,
                payload,
                kafka_topic,
                idempotency_key,
                status,
                retry_count,
                max_retries,
                created_at,
                version
            )
            VALUES (
                :id,
                :eventType,
                :payload,
                :kafkaTopic,
                :idempotencyKey,
                :status,
                :retryCount,
                :maxRetries,
                :createdAt,
                :version
            )
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(
            @Param("id") UUID id,
            @Param("eventType") String eventType,
            @Param("payload") String payload,
            @Param("kafkaTopic") String kafkaTopic,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("status") String status,
            @Param("retryCount") int retryCount,
            @Param("maxRetries") int maxRetries,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("version") Long version
    );

    @Query("""
                SELECT o FROM OutboxEvent o
                WHERE o.status = :status
                ORDER BY o.createdAt ASC
                LIMIT :limit
            """)
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(@Param("status") OutboxEventStatus status, @Param("limit") int limit);

    @Query("""
                SELECT o FROM OutboxEvent o
                WHERE o.status = :status
                  AND o.retryCount < o.maxRetries
                ORDER BY o.updatedAt ASC
                LIMIT :limit
            """)
    List<OutboxEvent> findRetryableEvents(@Param("status") OutboxEventStatus status, @Param("limit") int limit);

    @Modifying
    @Query("""
                DELETE FROM OutboxEvent o
                WHERE o.status = :status
                  AND o.processedAt < :cutoff
            """)
    int deleteProcessedBefore(@Param("status") OutboxEventStatus status, @Param("cutoff") LocalDateTime cutoff);
}
