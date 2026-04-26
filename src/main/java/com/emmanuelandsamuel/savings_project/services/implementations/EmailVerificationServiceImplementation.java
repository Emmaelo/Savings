package com.emmanuelandsamuel.savings_project.services.implementations;

import com.emmanuelandsamuel.savings_project.dtos.events.EmailVerificationEvent;
import com.emmanuelandsamuel.savings_project.dtos.requests.*;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.entities.EmailVerification;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import com.emmanuelandsamuel.savings_project.repositories.EmailVerificationRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.EmailVerificationService;
import com.emmanuelandsamuel.savings_project.services.interfaces.IdempotencyService;
import com.emmanuelandsamuel.savings_project.services.interfaces.OutboxEventService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImplementation implements EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;

    private final IdempotencyService idempotencyService;

    private final PasswordEncoder passwordEncoder;

    private final OutboxEventService outboxEventService;

    @Transactional
    @Override
    public ApiResponse<String> sendVerificationEmail(String idempotencyKey, String email) {

        try {

            /*
               Step 1: Check if key exists
             */

            IdempotencyKeyCheckRequest<String> idempotencyKeyCheckRequest = IdempotencyKeyCheckRequest.<String>builder()
                    .idempotencyKey(idempotencyKey)
                    .eventType(EMAIL_VERIFICATION_EVENT)
                    .incomingFingerprint(generateHash(email))
                    .responseType(String.class)
                    .build();

            Optional<ApiResponse<String>> existingResponse = idempotencyService.checkKey(idempotencyKeyCheckRequest);

            /*
               Step 2: If key exists return the response associated with that key
             */

            if (existingResponse.isPresent())
                return existingResponse.get();

            /*
                Step 3: Save idempotency key with request fingerprint
             */

            SaveIdempotencyKeyRequest saveIdempotencyKeyRequest = SaveIdempotencyKeyRequest
                    .builder()
                    .id(UUID.randomUUID())
                    .idempotencyKey(idempotencyKey)
                    .eventType(EMAIL_VERIFICATION_EVENT)
                    .requestFingerprint(generateHash(email))
                    .responseMessage("Your request is currently processing. Check your email address for a verification code.")
                    .expiresAt(LocalDateTime.now().plusMinutes(20L))
                    .build();

            int insertedIdempotencyRecordRows = idempotencyService.saveKey(saveIdempotencyKeyRequest);

            /*
                Step 4: in case of race condition / concurrent request if the key has already been inserted, return a
                message that the request is already processing
             */

            if (insertedIdempotencyRecordRows == 0) {

                log.info("Duplicate request for the same idempotency key and event type");

                return ApiResponse.success("Request is processing. Please check your email address for your verification code.");

            }

            /*
                 Step 5: Generate verification code
             */

            String verificationCode = generateVerificationCode();

            /*
                 Step 6: Fetch email verification record by email
             */

            Optional<EmailVerification> optionalEmailVerification = emailVerificationRepository.findByEmail(email);

            if (optionalEmailVerification.isEmpty()) {

                /*
                      Step 7: If there is no email verification by the given email, create a new email verification
                      record.
                 */

                SaveEmailVerificationRecordRequest saveEmailVerificationRecordRequest = SaveEmailVerificationRecordRequest
                        .builder()
                        .id(UUID.randomUUID())
                        .email(email)
                        .hashedVerificationCode(passwordEncoder.encode(verificationCode))
                        .build();

                /*
                      Step 8: Insert the email verfication record
                 */

                int insertedEmailVerificationRows = emailVerificationRepository.saveEmailVerificationIgnoreConflict(
                        saveEmailVerificationRecordRequest.getId(),
                        saveEmailVerificationRecordRequest.getEmail(),
                        saveEmailVerificationRecordRequest.getHashedVerificationCode(),
                        saveEmailVerificationRecordRequest.isVerified(),
                        saveEmailVerificationRecordRequest.getVersion(),
                        saveEmailVerificationRecordRequest.getCreatedAt(),
                        saveEmailVerificationRecordRequest.getExpiresAt()
                );

                /*
                    Step 9: In cases of concurrent request with different idempotency key but same email, we detect
                    this scenario and tell the user that the request is processing
                 */

                if (insertedEmailVerificationRows == 0) {

                    log.error("Concurrent insert detected for email {}. Another request has already created a verification record for this email.", email);

                    return ApiResponse.success("Your request is currently processing. Check your email address for a verification code.");

                }

            } else {

                /*
                    Step 10: In situations where there is an existing email verification record,
                    we update the existing record
                 */

                try {

                    EmailVerification emailVerification = optionalEmailVerification.get();

                    emailVerification.setVerified(false);

                    emailVerification.setToken(passwordEncoder.encode(verificationCode));

                    emailVerification.setExpiresAt(LocalDateTime.now().plusMinutes(15L));

                    emailVerificationRepository.save(emailVerification);

                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {

                    /*
                        Step 11: In situation where there is an optimistic lock exception because of the
                        @Version we added to our entity we tell the user that the request is currently being processed
                     */

                    return ApiResponse.success("Your request is currently processing. Check your email address for a verification code.");

                }
            }

            /*
               Step 12: Create an outbox event to send the email topic to kafka for processing
             */

            EmailVerificationEvent emailVerificationEvent = new EmailVerificationEvent(email, verificationCode);

            String jsonPayload = serialize(emailVerificationEvent);

            SaveOutboxEventRequest saveOutboxEventRequest = SaveOutboxEventRequest
                    .builder()
                    .id(UUID.randomUUID())
                    .eventType(EMAIL_VERIFICATION_EVENT)
                    .payload(jsonPayload)
                    .kafkaTopic(EMAIL_VERIFICATION_KAFKA_TOPIC)
                    .idempotencyKey(generateHash(Objects.requireNonNull(jsonPayload)))
                    .build();

            /*
                 Step 13: Save the outbox event to a table
             */

            outboxEventService.saveOutboxEvent(saveOutboxEventRequest);

            MarkIdempotencyKeyAsSuccessRequest<ApiResponse<String>> markIdempotencyKeyAsSuccessRequest =
                    MarkIdempotencyKeyAsSuccessRequest.<ApiResponse<String>>builder()
                            .idempotencyKey(idempotencyKey)
                            .eventType(EMAIL_VERIFICATION_EVENT)
                            .responseMessage("Verification code sent successfully to email address %s".formatted(email))
                            .responseBody(null)
                            .build();

            /*
                 Step 14: Update the saved idempotency record and mark the key as success
             */

            idempotencyService.markKeyAsSuccess(markIdempotencyKeyAsSuccessRequest);

            /*
                 Step 15: Return a successful response
             */

            return ApiResponse.success("Verification code sent successfully to email address %s".formatted(email));

        } catch (Exception ex) {

            log.error("An error occurred while sending verification email to {}: {}", email, ex.getMessage());

            throw new ApplicationException("Failed to send verification email. Please try again later.");

        }
    }

    @Override
    public ApiResponse<String> verifyVerificationCode(EmailVerificationRequest emailVerificationRequest) {

        try {

                /*
                Step 1: Fetch email verification record by email
                */

            Optional<EmailVerification> optionalEmailVerification = emailVerificationRepository.findByEmail(emailVerificationRequest.getEmail());

            if (optionalEmailVerification.isEmpty())
                return ApiResponse.error("No verification record found for the provided email address.");

            EmailVerification emailVerification = optionalEmailVerification.get();

            /*
                Step 2: Check if the provided verification code matches the hashed verification code in the database
                and also check if the verification code has expired. If the code is invalid or has expired, return an error response.
            */

            if (emailVerification.isVerified())
                return ApiResponse.success("Email verified successfully.");

            boolean isExpired = emailVerification.getExpiresAt() != null && emailVerification.getExpiresAt().isBefore(LocalDateTime.now());

            if (isExpired)
                return ApiResponse.error("Verification code has expired. Please request a new one.");

            boolean isCodeValid = passwordEncoder.matches(emailVerificationRequest.getVerificationCode(), emailVerification.getToken());

            if (!isCodeValid)
                return ApiResponse.error("Invalid verification code.");

            /*
                Step 3: Mark the email as verified
            */

            emailVerification.setVerified(true);

            emailVerificationRepository.save(emailVerification);

            return ApiResponse.success("Email verified successfully.");

        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {

            Optional<EmailVerification> optional = emailVerificationRepository.findByEmail(emailVerificationRequest.getEmail());

            if (optional.isPresent()) {

                EmailVerification emailVerification = optional.get();

                if (emailVerification.isVerified()) {

                    log.info("Email {} already verified by another request.", emailVerificationRequest.getEmail());

                    return ApiResponse.success("Email verified successfully.");

                }
            }

            log.error("Optimistic lock exception for email {}: {}", emailVerificationRequest.getEmail(), ex.getMessage());

            return ApiResponse.error("Failed to verify email. Please try again.");

        }
    }

    @Override
    public boolean isEmailVerified(String email) {

        Optional<EmailVerification> optionalEmailVerification = emailVerificationRepository.findByEmail(email);

        return optionalEmailVerification.map(EmailVerification::isVerified).orElse(false);

    }
}
