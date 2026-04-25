package com.emmanuelandsamuel.savings_project.enumerations;

public enum OutboxEventStatus {

    PENDING,
    PROCESSED,
    FAILED,
    DEAD_LETTER

}
