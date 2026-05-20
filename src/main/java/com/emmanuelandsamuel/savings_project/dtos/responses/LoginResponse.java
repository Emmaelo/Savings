package com.emmanuelandsamuel.savings_project.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    public UUID userId;

    public String jwtToken;

    public LocalDateTime loginDate;
}
