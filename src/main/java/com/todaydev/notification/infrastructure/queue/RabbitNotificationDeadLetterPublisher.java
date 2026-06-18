package com.todaydev.notification.infrastructure.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaydev.common.config.properties.NotificationProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@ConditionalOnProperty(name = "notification.queue.provider", havingValue = "rabbitmq")
public class RabbitNotificationDeadLetterPublisher implements NotificationDeadLetterPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final NotificationProperties properties;
    private final ObjectMapper objectMapper;

    public RabbitNotificationDeadLetterPublisher(
            RabbitTemplate rabbitTemplate,
            NotificationProperties properties,
            ObjectMapper objectMapper
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> publish(NotificationQueueMessage message, Throwable cause) {
        return Mono.fromRunnable(() -> rabbitTemplate.convertAndSend(
                        properties.queue().rabbitmq().exchange(),
                        RabbitNotificationConfig.DLQ_ROUTING_KEY,
                        serialize(message),
                        amqpMessage -> {
                            amqpMessage.getMessageProperties().setHeader("trace-id", message.traceId());
                            amqpMessage.getMessageProperties().setHeader("failure-message", cause.getMessage());
                            return amqpMessage;
                        }
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(AmqpException.class,
                        exception -> new TodaydevException(ErrorCode.NOTIFICATION_DLQ_PUBLISHED))
                .then();
    }

    private String serialize(NotificationQueueMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new TodaydevException(ErrorCode.NOTIFICATION_DLQ_PUBLISHED);
        }
    }
}
