package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.request.CreateConcertRequest;
import com.ticketbooking.concert_booking_platform.dto.request.UpdateConcertRequest;
import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.entity.TicketCategory;
import com.ticketbooking.concert_booking_platform.entity.User;
import com.ticketbooking.concert_booking_platform.enums.ConcertStatus;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.ConcertRepository;
import com.ticketbooking.concert_booking_platform.repository.TicketCategoryRepository;
import com.ticketbooking.concert_booking_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final UserRepository userRepository;


    /**
     * Cached: this is a read-heavy, low-mutation-frequency endpoint (concert
     * list changes only when an operator publishes/cancels). Key includes
     * page/size so pagination doesn't collide.
     */
    @Cacheable(value = "concerts", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Concert> browseOnSaleConcerts(Pageable pageable) {
        return concertRepository.findByStatus(ConcertStatus.ON_SALE, pageable);
    }


    /**
     * Cached: individual concert detail. NOTE — this includes
     * availableQuantity snapshots, which may be stale by up to the cache
     * TTL. This is acceptable for a browse/display endpoint; actual
     * reservation logic in BookingTransactionExecutor always reads fresh
     * via findByIdForUpdate() and never touches this cache, so overselling
     * risk is unaffected by cache staleness here.
     */
    @Cacheable(value = "concertDetail", key = "#concertId")
    public Concert getConcertDetail(Long concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found: " + concertId));
    }

    public java.util.List<com.ticketbooking.concert_booking_platform.entity.TicketCategory> getTicketCategories(Long concertId) {
        return ticketCategoryRepository.findByConcertId(concertId);
    }


    /** Admin browse - list all concerts regardless of status. */
    public Page<Concert> getAllConcerts(Pageable pageable) {
        return concertRepository.findAll(pageable);
    }


    /**
     * Evicts both caches on any mutating admin action, since publishing/
     * cancelling/updating a concert can change what shows up in the browse
     * list AND the detail view. Simpler and safer than fine-grained
     * per-entry eviction for this scope.
     */
    @CacheEvict(value = {"concerts", "concertDetail"}, allEntries = true)
    @Transactional
    public Concert createConcert(CreateConcertRequest request, Long createdByUserId) {
        User creator = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + createdByUserId));

        Concert concert = Concert.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .venue(request.getVenue())
                .eventDate(request.getEventDate())
                .status(ConcertStatus.UPCOMING) // Always starts as UPCOMING
                .createdBy(creator)
                .build();
        Concert saved = concertRepository.save(concert);

        List<TicketCategory> categories = request.getTicketCategories().stream()
                .map(tc -> TicketCategory.builder()
                        .concert(saved)
                        .name(tc.getName())
                        .price(tc.getPrice())
                        .totalQuantity(tc.getTotalQuantity())
                        .availableQuantity(tc.getTotalQuantity()) // Starts fully available
                        .build())
                .toList();
        ticketCategoryRepository.saveAll(categories);

        return saved;
    }


    @CacheEvict(value = {"concerts", "concertDetail"}, allEntries = true)
    @Transactional
    public Concert updateConcert(Long concertId, UpdateConcertRequest request) {
        Concert concert = getConcertDetail(concertId);
        if (request.getTitle() != null) concert.setTitle(request.getTitle());
        if (request.getDescription() != null) concert.setDescription(request.getDescription());
        if (request.getVenue() != null) concert.setVenue(request.getVenue());
        if (request.getEventDate() != null) concert.setEventDate(request.getEventDate());
        return concertRepository.save(concert);
    }

    /**
     * Explicit publish action: UPCOMING -> ON_SALE.
     */
    @CacheEvict(value = {"concerts", "concertDetail"}, allEntries = true)
    @Transactional
    public Concert publishConcert(Long concertId) {
        Concert concert = getConcertDetail(concertId);
        if (concert.getStatus() != ConcertStatus.UPCOMING) {
            throw new IllegalStateException(
                    "Only UPCOMING concerts can be published, current status: " + concert.getStatus());
        }
        concert.setStatus(ConcertStatus.ON_SALE);
        return concertRepository.save(concert);
    }


    @CacheEvict(value = {"concerts", "concertDetail"}, allEntries = true)
    @Transactional
    public Concert cancelConcert(Long concertId) {
        Concert concert = getConcertDetail(concertId);
        if (concert.getStatus() == ConcertStatus.ENDED) {
            throw new IllegalStateException("Cannot cancel a concert that has already ended");
        }
        concert.setStatus(ConcertStatus.CANCELLED);
        return concertRepository.save(concert);
    }
}