package com.emmanuelandsamuel.savings_project.entities;

import com.emmanuelandsamuel.savings_project.enumerations.OutboxEventStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_events",
        indexes = {

                @Index(name = "idx_outbox_status_created", columnList = "status, createdAt"),

        },
        uniqueConstraints = {

                @UniqueConstraint(name = "uq_outbox_idempotency_key", columnNames = "idempotencyKey")

        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 200)
    private String kafkaTopic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int maxRetries = 5;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    private LocalDateTime processedAt;

    public void incrementRetry(String errorMessage) {

        this.retryCount++;

        this.lastError = errorMessage;

        if (this.retryCount >= this.maxRetries) {

            this.status = OutboxEventStatus.DEAD_LETTER;

        } else {

            this.status = OutboxEventStatus.FAILED;
        }
    }

    public void markProcessed() {

        this.status = OutboxEventStatus.PROCESSED;

        this.processedAt = LocalDateTime.now();
    }
}
