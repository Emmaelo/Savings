package com.emmanuelandsamuel.savings_project.producers.interfaces;

import java.util.concurrent.CompletableFuture;

public interface KafkaProducerService {

    CompletableFuture<Boolean> sendMessage(String topic, String key, Object message);

}
