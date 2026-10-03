package com.ticketbooking.concert_booking_platform.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "booking.events.exchange";
    public static final String NOTIFICATION_QUEUE = "booking.notification.queue";
    public static final String ROUTING_KEY = "booking.bookingconfirmed";

    @Bean
    public TopicExchange bookingEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange bookingEventsExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(bookingEventsExchange)
                .with(ROUTING_KEY);
    }
}