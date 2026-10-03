package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.request.CreateSeatZoneRequest;
import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.entity.Seat;
import com.ticketbooking.concert_booking_platform.entity.SeatRow;
import com.ticketbooking.concert_booking_platform.entity.SeatZone;
import com.ticketbooking.concert_booking_platform.enums.ConcertStatus;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.ConcertRepository;
import com.ticketbooking.concert_booking_platform.repository.SeatRepository;
import com.ticketbooking.concert_booking_platform.repository.SeatRowRepository;
import com.ticketbooking.concert_booking_platform.repository.SeatZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatmapAdminService {

    private final ConcertRepository concertRepository;
    private final SeatZoneRepository seatZoneRepository;
    private final SeatRowRepository seatRowRepository;
    private final SeatRepository seatRepository;

    /**
     * Bulk-creates a seating zone with all its rows and individual seats in
     * one call. Only allowed while the concert has not yet gone ON_SALE —
     * same restriction as updateConcert()/comp_quantity changes elsewhere:
     * once real customers may already hold or own seats, restructuring the
     * grid would corrupt those references.
     */
    @Transactional
    public SeatZone createSeatZone(Long concertId, CreateSeatZoneRequest request) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found: " + concertId));

        if (concert.getStatus() == ConcertStatus.ON_SALE || concert.getStatus() == ConcertStatus.ENDED) {
            throw new IllegalStateException(
                    "Cannot add or modify seat zones once the concert is ON_SALE or has ENDED");
        }

        int totalSeats = request.getRows().stream()
                .mapToInt(CreateSeatZoneRequest.RowDefinition::getSeatCount)
                .sum();

        SeatZone zone = SeatZone.builder()
                .concert(concert)
                .name(request.getName())
                .price(request.getPrice())
                .originalPrice(request.getPrice())
                .totalQuantity(totalSeats)
                .build();
        SeatZone savedZone = seatZoneRepository.save(zone);

        for (CreateSeatZoneRequest.RowDefinition rowDef : request.getRows()) {
            SeatRow row = SeatRow.builder()
                    .zone(savedZone)
                    .rowLabel(rowDef.getRowLabel())
                    .rowPriority(rowDef.getRowPriority())
                    .seatCount(rowDef.getSeatCount())
                    .build();
            SeatRow savedRow = seatRowRepository.save(row);

            List<Seat> seats = new ArrayList<>();
            for (int seatNumber = 1; seatNumber <= rowDef.getSeatCount(); seatNumber++) {
                seats.add(Seat.builder()
                        .row(savedRow)
                        .seatNumber(seatNumber)
                        .status("AVAILABLE")
                        .build());
            }
            seatRepository.saveAll(seats);
        }

        log.info("Created seat zone '{}' for concert {} with {} rows, {} total seats",
                request.getName(), concertId, request.getRows().size(), totalSeats);

        return savedZone;
    }

    @Transactional(readOnly = true)
    public List<SeatZone> getZonesByConcert(Long concertId) {
        return seatZoneRepository.findByConcertId(concertId);
    }
}