package com.emmanuelandsamuel.savings_project.producers.implementations;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.emmanuelandsamuel.savings_project.producers.interfaces.RabbitProducerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitProducerImplementation implements RabbitProducerService {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    private final RabbitTemplate rabbitTemplate;


     public boolean sendJsonMessage(Object message){
        
        log.info(String.format("Json Message sent -> %s", message.toString()));
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);

        return true;
    }

}
