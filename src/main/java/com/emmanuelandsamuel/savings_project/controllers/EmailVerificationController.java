package com.emmanuelandsamuel.savings_project.controllers;

import com.emmanuelandsamuel.savings_project.dtos.requests.EmailVerificationRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.services.interfaces.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.IDEMPOTENCY_KEY_HEADER;

@SuppressWarnings("NullableProblems")
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Tag(name = "Email verification", description = "APIs for email verification")
public class EmailVerificationController {

    /*
    Use this url to lauch swagger ui: http://localhost:8080/swagger-ui/index.html
     */

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "Send verification mail", description = "Sends a verification email to the specified email address. The email contains a verification code that the user can use to verify their email address.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification email sent successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/send-verification-code")
    public ResponseEntity<ApiResponse<String>> sendVerificationEmail(@RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                                                                     @RequestParam("email") String email) {

        if (idempotencyKey == null || idempotencyKey.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("Idempotency key is required"));

        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("Email is required"));

        ApiResponse<String> apiResponse = emailVerificationService.sendVerificationEmail(idempotencyKey, email);

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);

    }

    @Operation(summary = "Verify verification code", description = "Verifies the provided verification code for the specified email address. If the verification code is valid, the email address is marked as verified.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification successful",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/verify-verification-code")
    public ResponseEntity<ApiResponse<String>> verifyVerificationCode(@Valid @RequestBody EmailVerificationRequest emailVerificationRequest) {

        ApiResponse<String> apiResponse = emailVerificationService.verifyVerificationCode(emailVerificationRequest);

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);
    }
}
