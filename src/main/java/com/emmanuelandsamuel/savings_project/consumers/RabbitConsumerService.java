package com.emmanuelandsamuel.savings_project.consumers;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitConsumerService {

     @RabbitListener(queues = {"${rabbitmq.queue.name}"})
    public void consumeJsonMessage(Object message){
        log.info(String.format("Received message -> %s",String.valueOf(message) ));
    }


}
