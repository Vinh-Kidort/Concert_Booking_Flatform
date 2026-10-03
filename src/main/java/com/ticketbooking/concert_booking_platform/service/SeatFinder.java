package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.entity.Seat;
import com.ticketbooking.concert_booking_platform.entity.SeatRow;
import com.ticketbooking.concert_booking_platform.repository.SeatRepository;
import com.ticketbooking.concert_booking_platform.repository.SeatRowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SeatFinder {

    private final SeatRowRepository seatRowRepository;
    private final SeatRepository seatRepository;

    public record SeatCandidateResult(List<Long> seatIds, boolean isSplit, int groupCount) {}

    /**
     * Best-Available-Seat search, read-only (no locking here — candidates
     * are advisory; the caller must re-verify + lock before committing).
     * Priority order:
     *   1. A single row with N contiguous available seats (closest row first)
     *   2. Two adjacent-priority rows, matched by identical seat_number
     *      ("same column"), where each row contributes as many seats as
     *      it can
     *   3. Fewest possible groups overall, largest groups first
     */
    public Optional<SeatCandidateResult> findBestAvailableSeats(Long zoneId, int quantity) {
        List<SeatRow> rows = seatRowRepository.findByZoneIdOrderByRowPriorityAsc(zoneId);

        // Pass 1: single row, contiguous
        for (SeatRow row : rows) {
            List<Seat> available = seatRepository.findByRowIdAndStatusOrderBySeatNumberAsc(row.getId(), "AVAILABLE");
            Optional<List<Seat>> contiguous = findContiguousBlock(available, quantity);
            if (contiguous.isPresent()) {
                return Optional.of(new SeatCandidateResult(
                        contiguous.get().stream().map(Seat::getId).toList(), false, 1));
            }
        }

        // Pass 2: two adjacent-priority rows, matched by seat_number
        for (int i = 0; i < rows.size() - 1; i++) {
            SeatRow rowA = rows.get(i);
            SeatRow rowB = rows.get(i + 1);

            List<Seat> availA = seatRepository.findByRowIdAndStatusOrderBySeatNumberAsc(rowA.getId(), "AVAILABLE");
            List<Seat> availB = seatRepository.findByRowIdAndStatusOrderBySeatNumberAsc(rowB.getId(), "AVAILABLE");

            if (availA.size() + availB.size() < quantity) continue;

            Set<Integer> seatNumbersA = availA.stream().map(Seat::getSeatNumber).collect(Collectors.toSet());
            Set<Integer> seatNumbersB = availB.stream().map(Seat::getSeatNumber).collect(Collectors.toSet());

            // Only consider seat_numbers that exist as AVAILABLE in both rows
            // for "same column" matching — rows may have different lengths.
            List<Integer> commonColumns = seatNumbersA.stream()
                    .filter(seatNumbersB::contains)
                    .sorted()
                    .toList();

            if (commonColumns.size() >= Math.min(quantity, 2)) {
                // Split: take as many as fit from rowA first (matching columns),
                // remainder from rowB, same columns preferred.
                List<Seat> selected = new ArrayList<>();
                for (Integer col : commonColumns) {
                    if (selected.size() >= quantity) break;
                    availA.stream().filter(s -> s.getSeatNumber().equals(col)).findFirst().ifPresent(selected::add);
                }
                for (Integer col : commonColumns) {
                    if (selected.size() >= quantity) break;
                    availB.stream().filter(s -> s.getSeatNumber().equals(col))
                            .filter(s -> selected.stream().noneMatch(sel -> sel.getId().equals(s.getId())))
                            .findFirst().ifPresent(selected::add);
                }
                if (selected.size() == quantity) {
                    return Optional.of(new SeatCandidateResult(
                            selected.stream().map(Seat::getId).toList(), true, 2));
                }
            }
        }

        // Pass 3: fewest groups, largest first — take greedily row by row
        List<Seat> allSelected = new ArrayList<>();
        for (SeatRow row : rows) {
            if (allSelected.size() >= quantity) break;
            List<Seat> available = seatRepository.findByRowIdAndStatusOrderBySeatNumberAsc(row.getId(), "AVAILABLE");
            int needed = quantity - allSelected.size();
            allSelected.addAll(available.stream().limit(needed).toList());
        }

        if (allSelected.size() == quantity) {
            long distinctRows = allSelected.stream().map(s -> s.getRow().getId()).distinct().count();
            return Optional.of(new SeatCandidateResult(
                    allSelected.stream().map(Seat::getId).toList(), distinctRows > 1, (int) distinctRows));
        }

        return Optional.empty(); // không đủ ghế trống trong toàn zone
    }

    private Optional<List<Seat>> findContiguousBlock(List<Seat> sortedAvailable, int quantity) {
        if (sortedAvailable.size() < quantity) return Optional.empty();

        for (int start = 0; start <= sortedAvailable.size() - quantity; start++) {
            List<Seat> window = sortedAvailable.subList(start, start + quantity);
            boolean contiguous = true;
            for (int i = 1; i < window.size(); i++) {
                if (window.get(i).getSeatNumber() - window.get(i - 1).getSeatNumber() != 1) {
                    contiguous = false;
                    break;
                }
            }
            if (contiguous) return Optional.of(new ArrayList<>(window));
        }
        return Optional.empty();
    }
}