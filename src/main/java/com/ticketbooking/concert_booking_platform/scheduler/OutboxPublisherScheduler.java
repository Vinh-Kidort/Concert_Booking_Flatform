package com.ticketbooking.concert_booking_platform.scheduler;

import com.ticketbooking.concert_booking_platform.entity.OutboxEvent;
import com.ticketbooking.concert_booking_platform.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherScheduler {

    private static final int MAX_RETRY = 20;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : pending) {
            try {
                publishSingleEvent(event);
            } catch (Exception e) {
                handlePublishFailure(event, e);
            }
        }
    }

    @Transactional
    protected void publishSingleEvent(OutboxEvent event) {
        rabbitTemplate.convertAndSend(
                "booking.events.exchange",
                "booking." + event.getEventType().toLowerCase(),
                event.getPayload()
        );

        event.setStatus("PUBLISHED");
        event.setPublishedAt(OffsetDateTime.now());
        outboxEventRepository.save(event);

        log.info("Published outbox event id={} type={} to RabbitMQ", event.getId(), event.getEventType());
    }

    @Transactional
    protected void handlePublishFailure(OutboxEvent event, Exception e) {
        int newRetryCount = event.getRetryCount() + 1;
        event.setRetryCount(newRetryCount);

        if (newRetryCount >= MAX_RETRY) {
            event.setStatus("FAILED");
            log.error("Outbox event id={} failed permanently after {} retries", event.getId(), newRetryCount, e);
        } else {
            log.warn("Outbox event id={} publish failed (attempt {}/{}), will retry next cycle",
                    event.getId(), newRetryCount, MAX_RETRY, e);
        }
        outboxEventRepository.save(event);
    }
}