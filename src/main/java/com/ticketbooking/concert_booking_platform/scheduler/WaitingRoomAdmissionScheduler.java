package com.ticketbooking.concert_booking_platform.scheduler;

import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.enums.ConcertStatus;
import com.ticketbooking.concert_booking_platform.repository.ConcertRepository;
import com.ticketbooking.concert_booking_platform.service.WaitingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WaitingRoomAdmissionScheduler {

    private final WaitingRoomService waitingRoomService;
    private final ConcertRepository concertRepository;

    @Value("${app.waiting-room.enabled}")
    private boolean enabled;

    @Value("${app.waiting-room.admission-batch-size}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.waiting-room.admission-interval-ms:5000}")
    public void admitBatchForOnSaleConcerts() {
        if (!enabled) return;

        List<Concert> onSaleConcerts = concertRepository.findByStatus(ConcertStatus.ON_SALE,
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        for (Concert concert : onSaleConcerts) {
            waitingRoomService.admitNextBatch(concert.getId(), batchSize);
        }
    }
}