package com.emmanuelandsamuel.savings_project.entities;

import com.emmanuelandsamuel.savings_project.enumerations.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "idempotency_records",
        indexes = {
                @Index(name = "idx_idempotency_expires_at", columnList = "expiresAt"),

        },
        uniqueConstraints = @UniqueConstraint(name = "uq_idempotency_key_event", columnNames = {"key", "eventType"})
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class IdempotencyRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false)
    private String requestFingerprint;

    @Column(nullable = false)
    private String responseMessage;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @Enumerated(EnumType.STRING)
    private IdempotencyStatus idempotencyStatus;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {

        this.expiresAt = LocalDateTime.now().plusHours(24);

    }
}
