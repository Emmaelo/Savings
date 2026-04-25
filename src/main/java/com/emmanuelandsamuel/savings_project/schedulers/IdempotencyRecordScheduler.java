package com.emmanuelandsamuel.savings_project.schedulers;

import com.emmanuelandsamuel.savings_project.repositories.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyRecordScheduler {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    /*
     * This scheduled task runs every day at 2 AM and deletes all idempotency records that have expired
     * (i.e., their expiresAt timestamp is before the current time).
     */

    @Scheduled(cron = "0 0 2 * * *")
    public void deleteExpiredKeys() {

        log.info("Purging expired idempotency keys");

        idempotencyRecordRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());


    }
}
