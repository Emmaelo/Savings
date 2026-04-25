package com.emmanuelandsamuel.savings_project.entities;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "email_verifications",
        uniqueConstraints = {

                @UniqueConstraint(name = "uq_email_verification_email", columnNames = "email")

        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EmailVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private boolean isVerified;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

}
