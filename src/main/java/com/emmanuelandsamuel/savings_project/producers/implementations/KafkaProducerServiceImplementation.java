package com.emmanuelandsamuel.savings_project.producers.implementations;

import com.emmanuelandsamuel.savings_project.producers.interfaces.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("NullableProblems")
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerServiceImplementation implements KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public CompletableFuture<Boolean> sendMessage(String topic, String key, Object message) {

        return kafkaTemplate.send(topic, key, message)
                .thenApply(result -> {

                    log.info("Message sent successfully to topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );

                    return true;

                })
                .exceptionally(ex -> {

                    log.error("Failed to send Kafka message to topic={}, key={}", topic, key, ex);

                    return false;

                });
    }
}
