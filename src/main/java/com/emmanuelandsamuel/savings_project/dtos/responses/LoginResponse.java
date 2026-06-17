package com.emmanuelandsamuel.savings_project.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginResponse {

    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private BigDecimal balance;
    
    private String token;

    private LocalDateTime loginDate;
}
