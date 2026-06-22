package com.emmanuelandsamuel.savings_project.producers.interfaces;

public interface RabbitProducerService {
    boolean sendJsonMessage(Object message);

}
