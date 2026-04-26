package com.emmanuelandsamuel.savings_project.services.implementations;

import com.emmanuelandsamuel.savings_project.dtos.requests.*;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.enumerations.Role;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import com.emmanuelandsamuel.savings_project.repositories.UserRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.EmailVerificationService;
import com.emmanuelandsamuel.savings_project.services.interfaces.IdempotencyService;
import com.emmanuelandsamuel.savings_project.services.interfaces.UserService;
import com.emmanuelandsamuel.savings_project.services.interfaces.UserWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class UserServiceImplementation implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final IdempotencyService idempotencyService;

    private final EmailVerificationService emailVerificationService;

    private final UserWalletService userWalletService;

    @Transactional
    @Override
    public ApiResponse<String> registerUser(String idempotencyKey, UserRegistrationRequest userRegistrationRequest) {

        try {

            String jsonPayload = serialize(userRegistrationRequest);

            String requestFingerprint = generateHash(Objects.requireNonNull(jsonPayload));

            IdempotencyKeyCheckRequest<String> idempotencyKeyCheckRequest =
                    IdempotencyKeyCheckRequest.<String>builder()
                    .idempotencyKey(idempotencyKey)
                    .incomingFingerprint(requestFingerprint)
                    .eventType(USER_REGISTERED_EVENT)
                    .responseType(String.class)
                    .build();

            Optional<ApiResponse<String>> cachedResponse = idempotencyService.checkKey(idempotencyKeyCheckRequest);

            if (cachedResponse.isPresent()) {

                log.info("Idempotent request detected. Returning cached response for key: {}", idempotencyKey);

                return cachedResponse.get();

            }

            boolean isEmailVerified = emailVerificationService.isEmailVerified(userRegistrationRequest.getEmail());

            if (!isEmailVerified)
                return ApiResponse.error("Email address is not verified. Please verify your email before registering.");

            boolean emailExists = userRepository.existsByEmail(userRegistrationRequest.getEmail());

            if (emailExists)
                return ApiResponse.error("Email address is already in use. Please use a different email.");

            boolean doesPasswordMatch = userRegistrationRequest.getPassword().equals(userRegistrationRequest.getConfirmPassword());

            if (!doesPasswordMatch)
                return ApiResponse.error("Password and confirm password do not match.");

            SaveIdempotencyKeyRequest saveIdempotencyKeyRequest = SaveIdempotencyKeyRequest
                    .builder()
                    .id(UUID.randomUUID())
                    .idempotencyKey(idempotencyKey)
                    .eventType(USER_REGISTERED_EVENT)
                    .requestFingerprint(requestFingerprint)
                    .responseMessage("Your registration request is currently being processed. Please wait a moment and try again.")
                    .expiresAt(LocalDateTime.now().plusMinutes(20L))
                    .build();

            int insertedIdempotencyRecordRows = idempotencyService.saveKey(saveIdempotencyKeyRequest);

            if (insertedIdempotencyRecordRows == 0) {

                log.info("Another request with the same idempotency key is already being processed. IdempotencyKey: {}", idempotencyKey);

                return ApiResponse.success("Your registration request is currently being processed. Please wait a moment and try again.");

            }

            int insertedUserRows = userRepository.insertUserIgnoreConflict(
                    UUID.randomUUID(),
                    userRegistrationRequest.getEmail(),
                    userRegistrationRequest.getFirstname(),
                    userRegistrationRequest.getLastname(),
                    passwordEncoder.encode(userRegistrationRequest.getPassword()),
                    userRegistrationRequest.getPhoneNumber(),
                    0L,
                    false,
                    0,
                    Role.USER.name()
            );

            if (insertedUserRows == 0) {

                log.info(
                        "Concurrent registration attempt detected for email {}. IdempotencyKey: {}. Another request has already created a user record for this email.",
                        userRegistrationRequest.getEmail(),
                        idempotencyKey
                );

                MarkIdempotencyKeyAsFailedRequest markIdempotencyKeyAsFailedRequest = MarkIdempotencyKeyAsFailedRequest
                        .builder()
                        .idempotencyKey(idempotencyKey)
                        .eventType(USER_REGISTERED_EVENT)
                        .responseMessage("Email address is already in use. Please use a different email.")
                        .build();

                idempotencyService.markKeyAsFailed(markIdempotencyKeyAsFailedRequest);

                return ApiResponse.error("Email address is already in use. Please use a different email.");

            }

            userWalletService.createUserWallet(userRegistrationRequest.getEmail());

            MarkIdempotencyKeyAsSuccessRequest<String> markIdempotencyKeyAsSuccessRequest =
                    MarkIdempotencyKeyAsSuccessRequest.<String>builder()
                            .idempotencyKey(idempotencyKey)
                            .eventType(USER_REGISTERED_EVENT)
                            .responseMessage("User registered successfully.")
                            .responseBody(null)
                            .build();

            idempotencyService.markKeyAsSuccess(markIdempotencyKeyAsSuccessRequest);

            return ApiResponse.success("User registered successfully.");

        } catch (Exception ex) {

            log.error("An error occurred while registering user: {}", ex.getMessage(), ex);

            throw new ApplicationException("Registration failed. Please try again later.");

        }
    }
}
