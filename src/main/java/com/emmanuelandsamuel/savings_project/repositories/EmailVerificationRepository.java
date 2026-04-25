package com.emmanuelandsamuel.savings_project.repositories;

import com.emmanuelandsamuel.savings_project.entities.EmailVerification;
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
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    Optional<EmailVerification> findByEmail(String email);

    @Modifying
    @Query(value = """
            INSERT INTO email_verifications (
              id,
              email,
              token,
              is_verified,
              version,
              created_at,
              expires_at
            ) VALUES (
               :id,
               :email,
               :token,
               :isVerified,
               :version,
               :createdAt,
               :expiresAt
            )
            ON CONFLICT (email) DO NOTHING
            """, nativeQuery = true)
    int saveEmailVerificationIgnoreConflict(
            @Param("id") UUID id,
            @Param("email") String email,
            @Param("token") String hashedVerificationCode,
            @Param("isVerified") boolean isVerified,
            @Param("version") Long version,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("expiresAt") LocalDateTime expiresAt
    );

}
