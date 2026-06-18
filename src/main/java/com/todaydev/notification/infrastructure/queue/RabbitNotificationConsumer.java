package com.todaydev.notification.infrastructure.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaydev.notification.service.NotificationDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.queue.provider", havingValue = "rabbitmq")
public class RabbitNotificationConsumer implements NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitNotificationConsumer.class);

    private final NotificationDeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    public RabbitNotificationConsumer(NotificationDeliveryService deliveryService, ObjectMapper objectMapper) {
        this.deliveryService = deliveryService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(
            queues = "${notification.queue.rabbitmq.request-queue}",
            concurrency = "${notification.queue.consumer-concurrency}"
    )
    public void consumeRequested(String body) {
        process(deserialize(body));
    }

    private void process(NotificationQueueMessage message) {
        try {
            deliveryService.process(message).block();
        } catch (RuntimeException exception) {
            log.warn(
                    "Notification consumer failed after internal handling: deliveryId={}, messageId={}",
                    message.deliveryId(), message.messageId(), exception
            );
        }
    }

    private NotificationQueueMessage deserialize(String body) {
        try {
            return objectMapper.readValue(body, NotificationQueueMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid notification queue message", exception);
        }
    }
}
