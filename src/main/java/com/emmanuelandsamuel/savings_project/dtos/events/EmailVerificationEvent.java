package com.emmanuelandsamuel.savings_project.dtos.events;

public record EmailVerificationEvent(String email, String verificationCode) {
}
