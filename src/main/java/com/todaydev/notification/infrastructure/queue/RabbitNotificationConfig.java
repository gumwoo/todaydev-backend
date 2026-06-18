package com.todaydev.notification.infrastructure.queue;

import com.todaydev.common.config.properties.NotificationProperties;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "notification.queue.provider", havingValue = "rabbitmq")
public class RabbitNotificationConfig {

    public static final String REQUEST_ROUTING_KEY = "notification.requested";
    public static final String RETRY_ROUTING_KEY = "notification.retry";
    public static final String DLQ_ROUTING_KEY = "notification.dlq";

    @Bean
    public DirectExchange notificationExchange(NotificationProperties properties) {
        return new DirectExchange(properties.queue().rabbitmq().exchange(), true, false);
    }

    @Bean
    public Queue notificationRequestQueue(NotificationProperties properties) {
        return QueueBuilder.durable(properties.queue().rabbitmq().requestQueue()).build();
    }

    @Bean
    public Queue notificationRetryQueue(NotificationProperties properties) {
        return QueueBuilder.durable(properties.queue().rabbitmq().retryQueue())
                .withArguments(Map.of(
                        "x-message-ttl", properties.queue().retryBackoffMillis(),
                        "x-dead-letter-exchange", properties.queue().rabbitmq().exchange(),
                        "x-dead-letter-routing-key", REQUEST_ROUTING_KEY
                ))
                .build();
    }

    @Bean
    public Queue notificationDlq(NotificationProperties properties) {
        return QueueBuilder.durable(properties.queue().rabbitmq().dlq()).build();
    }

    @Bean
    public Binding notificationRequestBinding(
            Queue notificationRequestQueue,
            DirectExchange notificationExchange
    ) {
        return BindingBuilder.bind(notificationRequestQueue)
                .to(notificationExchange)
                .with(REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding notificationRetryBinding(
            Queue notificationRetryQueue,
            DirectExchange notificationExchange
    ) {
        return BindingBuilder.bind(notificationRetryQueue)
                .to(notificationExchange)
                .with(RETRY_ROUTING_KEY);
    }

    @Bean
    public Binding notificationDlqBinding(
            Queue notificationDlq,
            DirectExchange notificationExchange
    ) {
        return BindingBuilder.bind(notificationDlq)
                .to(notificationExchange)
                .with(DLQ_ROUTING_KEY);
    }

}
