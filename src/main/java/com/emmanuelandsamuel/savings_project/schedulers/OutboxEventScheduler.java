package com.emmanuelandsamuel.savings_project.schedulers;

import com.emmanuelandsamuel.savings_project.entities.OutboxEvent;
import com.emmanuelandsamuel.savings_project.enumerations.OutboxEventStatus;
import com.emmanuelandsamuel.savings_project.producers.interfaces.KafkaProducerService;
import com.emmanuelandsamuel.savings_project.repositories.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.emmanuelandsamuel.savings_project.utilities.AppExtensions.deserialize;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;

    private final KafkaProducerService kafkaProducerService;

    private final MeterRegistry meterRegistry;

    @Value("${outbox.poll.batch-size:50}")
    private int batchSize;

    @Value("${outbox.poll.backoff-multiplier-seconds:30}")
    private long backoffMultiplierSeconds;

    @Value("${outbox.poll.cleanup-retention-hours:24}")
    private long cleanupRetentionHours;

    private Counter publishedCounter;

    private Counter failedCounter;

    private Counter deadLetterCounter;

    private Counter skippedDuplicateCounter;

    @PostConstruct
    void initMetrics() {

        publishedCounter = meterRegistry.counter("outbox.events.published");

        failedCounter = meterRegistry.counter("outbox.events.failed");

        deadLetterCounter = meterRegistry.counter("outbox.events.dead_letter");

        skippedDuplicateCounter = meterRegistry.counter("outbox.events.skipped_duplicate");
    }

    @Scheduled(fixedDelayString = "${outbox.poll.pending-interval-ms:5000}")
    @Transactional
    public void processPendingEvents() {

        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, batchSize);

        if (!events.isEmpty()) {

            log.debug("Processing {} PENDING outbox events", events.size());

            events.forEach(this::publishEvent);
        }
    }

    @Scheduled(fixedDelayString = "${outbox.poll.retry-interval-ms:60000}")
    @Transactional
    public void retryFailedEvents() {

        List<OutboxEvent> events = outboxEventRepository.findRetryableEvents(OutboxEventStatus.FAILED, batchSize);

        if (!events.isEmpty()) {

            log.debug("Retrying {} FAILED outbox events", events.size());
        }

        for (OutboxEvent event : events) {

            if (!isBackoffElapsed(event)) {

                log.debug("Skipping event [id={}] — back-off not elapsed (attempt {})", event.getId(), event.getRetryCount());

                continue;
            }

            publishEvent(event);
        }
    }

    @Scheduled(fixedDelayString = "${outbox.poll.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupProcessedEvents() {

        LocalDateTime cutoff = LocalDateTime.now().minusHours(cleanupRetentionHours);

        int deleted = outboxEventRepository.deleteProcessedBefore(OutboxEventStatus.PROCESSED, cutoff);

        if (deleted > 0) {

            log.info("Cleaned up {} PROCESSED outbox events older than {} hours", deleted, cleanupRetentionHours);
        }
    }

    private void publishEvent(OutboxEvent event) {

        try {

            Object payload = deserialize(event.getPayload(), Object.class);

            boolean sent = kafkaProducerService.sendMessage(
                    event.getKafkaTopic(),
                    event.getId().toString(),
                    payload
            ).get();

            if (sent) {

                event.markProcessed();

                outboxEventRepository.save(event);

                publishedCounter.increment();

                log.info(
                        "Outbox event published [id={}, type={}, attempt={}]",
                        event.getId(),
                        event.getEventType(),
                        event.getRetryCount()
                );

            } else {

                handleFailure(event, "Broker may be unavailable");

            }

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            handleFailure(event, "Interrupted: " + ex.getMessage());

        } catch (ExecutionException ex) {

            handleFailure(event, ex.getCause().getMessage());

        } catch (ObjectOptimisticLockingFailureException ex) {

            /*
             * Another poller thread already processed this event between the
             * SELECT and this UPDATE. This is not an error — it is the @Version
             * guard working as intended. Log at DEBUG and skip.
             */

            skippedDuplicateCounter.increment();

            log.debug("Optimistic lock conflict on outbox event [id={}] — " + "already processed by another thread, skipping.", event.getId());

        } catch (Exception ex) {

            handleFailure(event, ex.getMessage());

        }
    }

    private void handleFailure(OutboxEvent event, String errorMessage) {

        log.warn("Outbox publish failed [id={}, type={}, attempt={}/{}]: {}", event.getId(), event.getEventType(),
                event.getRetryCount() + 1, event.getMaxRetries(), errorMessage);

        event.incrementRetry(errorMessage);

        outboxEventRepository.save(event);

        if (event.getStatus() == OutboxEventStatus.DEAD_LETTER) {

            deadLetterCounter.increment();

            log.error(
                    "Outbox event moved to DEAD_LETTER [id={}, type={}]. " + "Manual intervention required.",
                    event.getId(), event.getEventType()
            );

        } else {

            failedCounter.increment();

        }
    }

    private boolean isBackoffElapsed(OutboxEvent event) {

        long waitSeconds = (long) event.getRetryCount() * backoffMultiplierSeconds;

        LocalDateTime nextAttempt = event.getUpdatedAt().plusSeconds(waitSeconds);

        return LocalDateTime.now().isAfter(nextAttempt);
    }
}
