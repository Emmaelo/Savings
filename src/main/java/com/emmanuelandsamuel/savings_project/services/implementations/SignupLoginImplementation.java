package com.emmanuelandsamuel.savings_project.services.implementations;

import com.emmanuelandsamuel.savings_project.dtos.requests.*;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.LoginResponse;
import com.emmanuelandsamuel.savings_project.entities.UserEntity;
import com.emmanuelandsamuel.savings_project.enumerations.IdempotencyStatus;
import com.emmanuelandsamuel.savings_project.enumerations.Role;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import com.emmanuelandsamuel.savings_project.repositories.UserRepository;
import com.emmanuelandsamuel.savings_project.security.JWTGenerator;
import com.emmanuelandsamuel.savings_project.services.interfaces.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
public class SignupLoginImplementation implements SignupLoginService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final IdempotencyService idempotencyService;

    private final EmailVerificationService emailVerificationService;

    private final UserWalletService userWalletService;

    private final AuthenticationManager authenticationManager;

    // private final JwtService jwtService;
    private final JWTGenerator jwtGenerator;

    public ApiResponse<LoginResponse> loginUser2(UserLoginRequest userLoginRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userLoginRequest.getEmail(), userLoginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserEntity user = userRepository
                .findByEmailIgnoreCase(userLoginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String jwtToken = jwtGenerator.generateToken(auth, 1000L);

        LoginResponse loginResponse = LoginResponse
                .builder()
                .email(user.getEmail())
                .firstName(user.getFirstname())
                .lastName(user.getLastname())
                .token(jwtToken)
                .balance(user.getUserWallet().getAvailableBalance())

                .build();

        return ApiResponse.success("Login successful.", loginResponse);

    }

    @Transactional
    public ApiResponse<String> registerUser2(UserRegistrationRequest request) {

        boolean doesPasswordMatch = request.getPassword().equals(request.getConfirmPassword());

        if (!doesPasswordMatch) {
            return ApiResponse.error("Both passwords do not match.");
        }

        boolean emailExists = userRepository.existsByEmail(request.getEmail());
        if (emailExists) {
            return ApiResponse.error("Email address is already in use. Please use a different email.");
        }

       UserEntity savedUser = userRepository.save(UserEntity.builder().email(request.getEmail())
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.USER)
                .build());

        if (savedUser.getId() != null) {
        userWalletService.createUserWallet(request.getEmail(), request.getPhoneNumber());
          return ApiResponse.success("User registered successfully. Please check your email to verify your account");
        }

        return ApiResponse.error("Error Registering User.");

    }

    // @Transactional
    // @Override
    // public ApiResponse<String> registerUser(String idempotencyKey,
    // UserRegistrationRequest userRegistrationRequest) {

    // try {

    // String jsonPayload = serialize(userRegistrationRequest);

    // String requestFingerprint =
    // generateHash(Objects.requireNonNull(jsonPayload));

    // IdempotencyKeyCheckRequest<String> idempotencyKeyCheckRequest =
    // IdempotencyKeyCheckRequest.<String>builder()
    // .idempotencyKey(idempotencyKey)
    // .incomingFingerprint(requestFingerprint)
    // .eventType(USER_REGISTERED_EVENT)
    // .responseType(String.class)
    // .build();

    // Optional<ApiResponse<String>> cachedResponse =
    // idempotencyService.checkKey(idempotencyKeyCheckRequest);

    // if (cachedResponse.isPresent()) {

    // log.info("Idempotent request detected. Returning cached response for key:
    // {}", idempotencyKey);

    // return cachedResponse.get();

    // }

    // boolean isEmailVerified =
    // emailVerificationService.isEmailVerified(userRegistrationRequest.getEmail());

    // if (!isEmailVerified)
    // return ApiResponse.error("Email address is not verified. Please verify your
    // email before registering.");

    // boolean emailExists =
    // userRepository.existsByEmail(userRegistrationRequest.getEmail());

    // if (emailExists)
    // return ApiResponse.error("Email address is already in use. Please use a
    // different email.");

    // boolean doesPasswordMatch =
    // userRegistrationRequest.getPassword().equals(userRegistrationRequest.getConfirmPassword());

    // if (!doesPasswordMatch)
    // return ApiResponse.error("Password and confirm password do not match.");

    // SaveIdempotencyKeyRequest saveIdempotencyKeyRequest =
    // SaveIdempotencyKeyRequest
    // .builder()
    // .id(UUID.randomUUID())
    // .idempotencyKey(idempotencyKey)
    // .eventType(USER_REGISTERED_EVENT)
    // .requestFingerprint(requestFingerprint)
    // .idempotencyStatus(IdempotencyStatus.PROCESSING)
    // .responseMessage("Your registration request is currently being processed.
    // Please wait a moment and try again.")
    // .expiresAt(LocalDateTime.now().plusMinutes(20L))
    // .build();

    // int insertedIdempotencyRecordRows =
    // idempotencyService.saveKey(saveIdempotencyKeyRequest);

    // if (insertedIdempotencyRecordRows == 0) {

    // log.info("Another request with the same idempotency key is already being
    // processed. IdempotencyKey: {}", idempotencyKey);

    // return ApiResponse.success("Your registration request is currently being
    // processed. Please wait a moment and try again.");

    // }

    // int insertedUserRows = userRepository.insertUserIgnoreConflict(
    // UUID.randomUUID(),
    // userRegistrationRequest.getEmail(),
    // userRegistrationRequest.getFirstname(),
    // userRegistrationRequest.getLastname(),
    // passwordEncoder.encode(userRegistrationRequest.getPassword()),
    // userRegistrationRequest.getPhoneNumber(),
    // false,
    // 0,
    // Role.USER.name()
    // );

    // if (insertedUserRows == 0) {

    // log.info(
    // "Concurrent registration attempt detected for email {}. IdempotencyKey: {}.
    // Another request has already created a user record for this email.",
    // userRegistrationRequest.getEmail(),
    // idempotencyKey
    // );

    // MarkIdempotencyKeyAsFailedRequest markIdempotencyKeyAsFailedRequest =
    // MarkIdempotencyKeyAsFailedRequest
    // .builder()
    // .idempotencyKey(idempotencyKey)
    // .eventType(USER_REGISTERED_EVENT)
    // .responseMessage("Email address is already in use. Please use a different
    // email.")
    // .build();

    // idempotencyService.markKeyAsFailed(markIdempotencyKeyAsFailedRequest);

    // return ApiResponse.error("Email address is already in use. Please use a
    // different email.");

    // }

    // userWalletService.createUserWallet(userRegistrationRequest.getEmail());

    // MarkIdempotencyKeyAsSuccessRequest<String> markIdempotencyKeyAsSuccessRequest
    // =
    // MarkIdempotencyKeyAsSuccessRequest.<String>builder()
    // .idempotencyKey(idempotencyKey)
    // .eventType(USER_REGISTERED_EVENT)
    // .responseMessage("User registered successfully.")
    // .responseBody(null)
    // .build();

    // idempotencyService.markKeyAsSuccess(markIdempotencyKeyAsSuccessRequest);

    // return ApiResponse.success("User registered successfully.");

    // } catch (Exception ex) {

    // log.error("An error occurred while registering user: {}", ex.getMessage(),
    // ex);

    // throw new ApplicationException("Registration failed. Please try again
    // later.");

    // }
    // }

    // @Transactional
    // @Override
    // public ApiResponse<LoginResponse> loginUser(String idempotencyKey,
    // UserLoginRequest userLoginRequest) {

    // Optional<UserEntity> optionalUser =
    // userRepository.findByEmailIgnoreCase(userLoginRequest.getEmail());

    // if (optionalUser.isEmpty())
    // return ApiResponse.error("Invalid email or password.");

    // UserEntity user = optionalUser.get();

    // boolean isAccountLocked = user.isAccountLocked();

    // if (isAccountLocked)
    // return ApiResponse.error("Your account is locked due to multiple failed login
    // attempts. Please try again later or reset your password.");

    // boolean doesPasswordMatch =
    // passwordEncoder.matches(userLoginRequest.getPassword(), user.getPassword());

    // if (!doesPasswordMatch) {

    // String jsonPayload = serialize(userLoginRequest);

    // String requestFingerprint =
    // generateHash(Objects.requireNonNull(jsonPayload));

    // IdempotencyKeyCheckRequest<LoginResponse> idempotencyKeyCheckRequest =
    // IdempotencyKeyCheckRequest.<LoginResponse>builder()
    // .idempotencyKey(idempotencyKey)
    // .incomingFingerprint(requestFingerprint)
    // .eventType(USER_LOGIN_EVENT)
    // .responseType(LoginResponse.class)
    // .build();

    // Optional<ApiResponse<LoginResponse>> cachedResponse =
    // idempotencyService.checkKey(idempotencyKeyCheckRequest);

    // if (cachedResponse.isPresent()) {

    // log.info("Idempotent failed login attempt detected. Returning cached response
    // for key: {}", idempotencyKey);

    // return cachedResponse.get();

    // }

    // int currentFailedLoginAttempts = user.getFailedLoginAttempts();

    // int updatedFailedLoginAttempts = currentFailedLoginAttempts + 1;

    // int remainingLoginAttempts = MAX_LOGIN_ATTEMPTS - updatedFailedLoginAttempts;

    // if (remainingLoginAttempts <= 0) {

    // user.setAccountLocked(true);

    // userRepository.save(user);

    // return ApiResponse.error("Your account has been locked due to too many failed
    // login attempts. Please reset your password or contact support.");

    // }

    // user.setFailedLoginAttempts(updatedFailedLoginAttempts);

    // userRepository.save(user);

    // SaveIdempotencyKeyRequest saveIdempotencyKeyRequest =
    // SaveIdempotencyKeyRequest
    // .builder()
    // .id(UUID.randomUUID())
    // .idempotencyKey(idempotencyKey)
    // .eventType(USER_LOGIN_EVENT)
    // .requestFingerprint(requestFingerprint)
    // .idempotencyStatus(IdempotencyStatus.FAILURE)
    // .responseMessage("Invalid email or password. You have " +
    // remainingLoginAttempts + " more attempt(s) before your account gets locked.")
    // .expiresAt(LocalDateTime.now().plusMinutes(20L))
    // .build();

    // int insertedIdempotencyRecordRows =
    // idempotencyService.saveKey(saveIdempotencyKeyRequest);

    // if (insertedIdempotencyRecordRows == 0) {

    // log.info("Another failed login attempt with the same idempotency key is
    // already being processed. IdempotencyKey: {}", idempotencyKey);

    // }

    // return ApiResponse.error("Invalid email or password. You have " +
    // remainingLoginAttempts + " more attempt(s) before your account gets
    // locked.");

    // }

    // user.setFailedLoginAttempts(0);

    // user.setAccountLocked(false);

    // var usernamePasswordAuthenticationToken = new
    // UsernamePasswordAuthenticationToken(userLoginRequest.getEmail(),
    // userLoginRequest.getPassword());

    // Authentication authentication =
    // authenticationManager.authenticate(usernamePasswordAuthenticationToken);

    // String jwtToken = jwtService.generateToken(authentication);

    // LoginResponse loginResponse = LoginResponse
    // .builder()
    // .userId(user.getId())
    // .token(jwtToken)
    // .loginDate(LocalDateTime.now())
    // .build();

    // return ApiResponse.success("Login successful.", loginResponse);

    // }
}
