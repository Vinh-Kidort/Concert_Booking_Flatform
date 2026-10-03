package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.entity.Seat;
import com.ticketbooking.concert_booking_platform.entity.SeatRow;
import com.ticketbooking.concert_booking_platform.repository.SeatRepository;
import com.ticketbooking.concert_booking_platform.repository.SeatRowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatFinderTest {

    @Mock private SeatRowRepository seatRowRepository;
    @Mock private SeatRepository seatRepository;
    @InjectMocks private SeatFinder seatFinder;

    @Test
    void findBestAvailableSeats_contiguousInSingleRow_returnsWithoutSplit() {
        SeatRow rowA = SeatRow.builder().id(1L).rowPriority(1).build();
        when(seatRowRepository.findByZoneIdOrderByRowPriorityAsc(100L)).thenReturn(List.of(rowA));
        when(seatRepository.findByRowIdAndStatusOrderBySeatNumberAsc(1L, "AVAILABLE"))
                .thenReturn(List.of(
                        seat(1L, rowA, 1), seat(2L, rowA, 2), seat(3L, rowA, 3), seat(4L, rowA, 4)));

        var result = seatFinder.findBestAvailableSeats(100L, 3);

        assertThat(result).isPresent();
        assertThat(result.get().isSplit()).isFalse();
        assertThat(result.get().seatIds()).hasSize(3);
    }

    @Test
    void findBestAvailableSeats_noContiguousBlock_fallsBackToSplitAcrossTwoRows() {
        SeatRow rowA = SeatRow.builder().id(1L).rowPriority(1).build();
        SeatRow rowB = SeatRow.builder().id(2L).rowPriority(2).build();
        when(seatRowRepository.findByZoneIdOrderByRowPriorityAsc(100L)).thenReturn(List.of(rowA, rowB));

        // Row A only has seat #5 free (not enough alone for quantity=2)
        when(seatRepository.findByRowIdAndStatusOrderBySeatNumberAsc(1L, "AVAILABLE"))
                .thenReturn(List.of(seat(10L, rowA, 5)));
        // Row B has seat #5 free too — same column
        when(seatRepository.findByRowIdAndStatusOrderBySeatNumberAsc(2L, "AVAILABLE"))
                .thenReturn(List.of(seat(20L, rowB, 5), seat(21L, rowB, 8)));

        var result = seatFinder.findBestAvailableSeats(100L, 2);

        assertThat(result).isPresent();
        assertThat(result.get().isSplit()).isTrue();
        assertThat(result.get().groupCount()).isEqualTo(2);
    }

    @Test
    void findBestAvailableSeats_notEnoughSeatsAnywhere_returnsEmpty() {
        SeatRow rowA = SeatRow.builder().id(1L).rowPriority(1).build();
        when(seatRowRepository.findByZoneIdOrderByRowPriorityAsc(100L)).thenReturn(List.of(rowA));
        when(seatRepository.findByRowIdAndStatusOrderBySeatNumberAsc(1L, "AVAILABLE"))
                .thenReturn(List.of(seat(1L, rowA, 1)));

        var result = seatFinder.findBestAvailableSeats(100L, 5);

        assertThat(result).isEmpty();
    }

    private Seat seat(Long id, SeatRow row, int number) {
        return Seat.builder().id(id).row(row).seatNumber(number).status("AVAILABLE").build();
    }
}