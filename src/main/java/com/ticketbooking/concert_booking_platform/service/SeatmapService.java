package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.entity.*;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.exception.SeatUnavailableException;
import com.ticketbooking.concert_booking_platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatmapService {

    private static final int MAX_RETRY = 5;
    private static final long RETRY_BACKOFF_MS = 80;
    private static final int PROPOSAL_TTL_MINUTES = 15;
    private static final int BOOKING_HOLD_MINUTES = 15;

    private final SeatFinder seatFinder;
    private final SeatRepository seatRepository;
    private final SeatProposalRepository seatProposalRepository;
    private final SeatZoneRepository seatZoneRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * Two-phase seat selection to avoid holding row-level locks while
     * scanning potentially thousands of seats:
     *   Phase 1 (read-only, no lock): SeatFinder proposes candidate seats.
     *   Phase 2 (locked, fast): re-verify + lock only the chosen seats,
     *      sorted ascending by id (deadlock-avoidance, same principle as
     *      ticket_categories locking elsewhere in this codebase).
     * If Phase 2 finds a candidate seat was taken between phases (a narrow
     * race window), retry the whole search up to MAX_RETRY times with a
     * small backoff, rather than failing immediately.
     */
    @Transactional
    public SeatProposal proposeSeats(Long userId, Long zoneId, int quantity) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            var candidateOpt = seatFinder.findBestAvailableSeats(zoneId, quantity);
            if (candidateOpt.isEmpty()) {
                throw new SeatUnavailableException("Not enough available seats in this zone");
            }
            var candidate = candidateOpt.get();

            List<Long> sortedIds = candidate.seatIds().stream().sorted().toList();
            List<Seat> lockedSeats = seatRepository.findByIdInForUpdate(sortedIds);

            boolean allStillAvailable = lockedSeats.size() == sortedIds.size()
                    && lockedSeats.stream().allMatch(s -> "AVAILABLE".equals(s.getStatus()));

            if (allStillAvailable) {
                lockedSeats.forEach(s -> s.setStatus("PROPOSED"));
                seatRepository.saveAll(lockedSeats);

                SeatProposal proposal = SeatProposal.builder()
                        .zoneId(zoneId)
                        .userId(userId)
                        .seatIds(sortedIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                        .isSplit(candidate.isSplit())
                        .groupCount(candidate.groupCount())
                        .status("PENDING")
                        .expiresAt(OffsetDateTime.now().plusHours(24))
                        .build();

                return seatProposalRepository.save(proposal);
            }

            log.warn("Seat proposal attempt {}/{} lost race for zone {}, retrying", attempt, MAX_RETRY, zoneId);
            if (attempt < MAX_RETRY) {
                sleepBackoff(attempt);
            }
        }

        throw new SeatUnavailableException(
                "Could not secure seats after " + MAX_RETRY + " attempts — high contention, please retry shortly");
    }

    /**
     * Converts a confirmed seat proposal into a real Booking, reusing the
     * exact same Booking/BookingItem/BookingStatus infrastructure as the
     * standing-zone flow (BookingTransactionExecutor) — expiry scheduler,
     * status transitions, voucher application, and payment webhooks all
     * work identically regardless of whether a booking's items came from a
     * ticket_category or a specific seat.
     *
     * Seats move PROPOSED -> HELD here (same hold semantics/TTL as
     * available_quantity holds elsewhere), and are only fully committed to
     * BOOKED once the booking itself reaches CONFIRMED — mirrored by
     * BookingService#updateStatus, which must also release seats back to
     * AVAILABLE if the booking expires/is cancelled/fails (see note below).
     */
    @Transactional
    public Booking confirmProposal(Long userId, Long proposalId, String idempotencyKey) {
        SeatProposal proposal = seatProposalRepository.findByIdAndUserId(proposalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat proposal not found"));

        if (!"PENDING".equals(proposal.getStatus())) {
            throw new IllegalStateException("Proposal is no longer pending (status: " + proposal.getStatus() + ")");
        }
        if (proposal.getExpiresAt().isBefore(OffsetDateTime.now().minusHours(12))) {
            proposal.setStatus("EXPIRED");
            seatProposalRepository.save(proposal);
            throw new IllegalStateException("Proposal has expired, please request seats again");
        }

        List<Long> seatIds = parseSeatIds(proposal.getSeatIds());
        List<Seat> seats = seatRepository.findByIdInForUpdate(seatIds);

        boolean allStillProposed = seats.stream().allMatch(s -> "PROPOSED".equals(s.getStatus()));
        if (!allStillProposed) {
            throw new IllegalStateException("One or more seats in this proposal are no longer held");
        }

        SeatZone zone = seatZoneRepository.findById(proposal.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat zone not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = Booking.builder()
                .user(user)
                .concert(zone.getConcert())
                .idempotencyKey(idempotencyKey)
                .status(BookingStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(BOOKING_HOLD_MINUTES))
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Seat seat : seats) {
            seat.setStatus("HELD");

            BookingItem item = BookingItem.builder()
                    .seat(seat)
                    .quantity(1) // mỗi seat = 1 vé, không dùng field quantity như category
                    .unitPrice(zone.getPrice())
                    .build();
            booking.addItem(item);
            totalAmount = totalAmount.add(zone.getPrice());
        }

        seatRepository.saveAll(seats);

        booking.setTotalAmount(totalAmount);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(totalAmount);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        // link seats back to their booking_item for traceability
        for (int i = 0; i < seats.size(); i++) {
            seats.get(i).setBookingItemId(savedBooking.getItems().get(i).getId());
        }
        seatRepository.saveAll(seats);

        proposal.setStatus("CONFIRMED");
        seatProposalRepository.save(proposal);

        log.info("Confirmed seat proposal {} into booking {}, {} seats", proposalId, savedBooking.getId(), seats.size());

        return savedBooking;
    }

    @Transactional
    public void rejectProposal(Long userId, Long proposalId) {
        SeatProposal proposal = seatProposalRepository.findByIdAndUserId(proposalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat proposal not found"));

        releaseProposalSeats(proposal);
    }

    private void releaseProposalSeats(SeatProposal proposal) {
        List<Long> seatIds = parseSeatIds(proposal.getSeatIds());
        List<Seat> seats = seatRepository.findByIdInForUpdate(seatIds);
        seats.forEach(s -> s.setStatus("AVAILABLE"));
        seatRepository.saveAll(seats);

        proposal.setStatus("REJECTED");
        seatProposalRepository.save(proposal);
    }

    private List<Long> parseSeatIds(String csv) {
        return java.util.Arrays.stream(csv.split(",")).map(Long::valueOf).sorted().toList();
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(RETRY_BACKOFF_MS * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}