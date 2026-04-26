package com.emmanuelandsamuel.savings_project.controllers;

import com.emmanuelandsamuel.savings_project.dtos.requests.UserRegistrationRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.services.interfaces.UserService;
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
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User Authentication", description = "APIs for user registration, login, and authentication")
public class UserController {

    /*
    Use this url to lauch swagger ui: http://localhost:8080/swagger-ui/index.html
     */

    private final UserService userService;

    @Operation(summary = "Create an account", description = "Registers a new user account with the provided details. The registration process includes sending a verification email to the user's email address. The user must verify their email before they can register.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerUser(@RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                                                            @Valid @RequestBody UserRegistrationRequest userRegistrationRequest) {

        ApiResponse<String> apiResponse = userService.registerUser(idempotencyKey, userRegistrationRequest);

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);

    }
}
