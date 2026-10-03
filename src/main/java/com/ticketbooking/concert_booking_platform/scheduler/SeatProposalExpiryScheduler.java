package com.ticketbooking.concert_booking_platform.scheduler;

import com.ticketbooking.concert_booking_platform.entity.Seat;
import com.ticketbooking.concert_booking_platform.entity.SeatProposal;
import com.ticketbooking.concert_booking_platform.repository.SeatProposalRepository;
import com.ticketbooking.concert_booking_platform.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatProposalExpiryScheduler {

    private final SeatProposalRepository seatProposalRepository;
    private final SeatRepository seatRepository;

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void releaseExpiredProposals() {
        List<SeatProposal> expired = seatProposalRepository.findByStatusAndExpiresAtBefore(
                "PENDING", OffsetDateTime.now());

        for (SeatProposal proposal : expired) {
            List<Long> seatIds = Arrays.stream(proposal.getSeatIds().split(","))
                    .map(Long::valueOf).sorted().toList();
            List<Seat> seats = seatRepository.findByIdInForUpdate(seatIds);
            seats.forEach(s -> s.setStatus("AVAILABLE"));
            seatRepository.saveAll(seats);

            proposal.setStatus("EXPIRED");
            seatProposalRepository.save(proposal);
        }
        if (!expired.isEmpty()) {
            log.info("Released {} expired seat proposals", expired.size());
        }
    }
}