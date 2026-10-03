package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.request.CreateConcertRequest;
import com.ticketbooking.concert_booking_platform.dto.request.UpdateConcertRequest;
import com.ticketbooking.concert_booking_platform.entity.*;
import com.ticketbooking.concert_booking_platform.enums.ConcertApprovalStatus;
import com.ticketbooking.concert_booking_platform.enums.ConcertStatus;
import com.ticketbooking.concert_booking_platform.exception.InvalidApprovalTransitionException;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRepository organizationRepository;


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


    @Transactional(readOnly = true)
    public Page<Concert> listByOrganizationMember(Long userId, Pageable pageable) {
        List<Long> orgIds = organizationMemberRepository.findByUserId(userId).stream()
                .map(OrganizationMember::getOrganizationId)
                .toList();
        return concertRepository.findByOrganizationIdIn(orgIds, pageable);
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
        User creator = userRepository.getReferenceById(createdByUserId);
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + request.getOrganizationId()));

        Concert concert = Concert.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .venue(request.getVenue())
                .eventDate(request.getEventDate())
                .status(ConcertStatus.UPCOMING)
                .approvalStatus(ConcertApprovalStatus.DRAFT)
                .createdBy(creator)
                .organization(organization)
                .build();
        Concert saved = concertRepository.save(concert);

        List<TicketCategory> categories = request.getTicketCategories().stream()
                .map(tc -> {
                    int compQty = tc.getCompQuantity() != null ? tc.getCompQuantity() : 0;
                    if (compQty > tc.getTotalQuantity()) {
                        throw new IllegalArgumentException(
                                "Comp quantity cannot exceed total quantity for category: " + tc.getName());
                    }
                    return TicketCategory.builder()
                            .concert(saved)
                            .name(tc.getName())
                            .price(tc.getPrice())
                            .originalPrice(tc.getPrice())
                            .totalQuantity(tc.getTotalQuantity())
                            .compQuantity(compQty)
                            .availableQuantity(tc.getTotalQuantity() - compQty) // KHÔNG bao gồm comp
                            .build();
                })
                .toList();
        ticketCategoryRepository.saveAll(categories);

        return saved;
    }


    /**
     * Editing is only allowed while the concert has not yet been published
     * for sale. Once ON_SALE (or ENDED), customers have already made
     * purchase decisions based on the published information — allowing
     * silent edits afterward would be misleading to customers and exposes
     * the organizer to potential legal/contractual risk.
     */
    @CacheEvict(value = {"concerts", "concertDetail"}, allEntries = true)
    @Transactional
    public Concert updateConcert(Long concertId, UpdateConcertRequest request) {
        Concert concert = getConcertDetail(concertId);

        if (concert.getStatus() == ConcertStatus.ON_SALE || concert.getStatus() == ConcertStatus.ENDED) {
            throw new IllegalStateException(
                    "Cannot modify concert details once it is ON_SALE or has ENDED.");
        }

        if (request.getTitle() != null) concert.setTitle(request.getTitle());
        if (request.getDescription() != null) concert.setDescription(request.getDescription());
        if (request.getVenue() != null) concert.setVenue(request.getVenue());
        if (request.getEventDate() != null) concert.setEventDate(request.getEventDate());
        return concertRepository.save(concert);
    }


    /** Operator submits a DRAFT (or previously REJECTED) concert for organizer review. */
    @Transactional
    public Concert submitForReview(Long concertId) {
        Concert concert = getConcertDetail(concertId);
        transitionApproval(concert, ConcertApprovalStatus.PENDING_REVIEW);
        return concertRepository.save(concert);
    }

    /** Organizer approves — only the concert's own organizer may call this. */
    @Transactional
    public Concert approveConcert(Long organizerId, Long concertId) {
        Concert concert = getConcertDetail(concertId);
        verifyOrganizationMembership(organizerId, concert);
        transitionApproval(concert, ConcertApprovalStatus.APPROVED);
        concert.setReviewedBy(userRepository.getReferenceById(organizerId));
        concert.setReviewedAt(OffsetDateTime.now());
        concert.setRejectionReason(null);
        return concertRepository.save(concert);
    }

    /** Organizer rejects — Operator must fix and resubmit. */
    @Transactional
    public Concert rejectConcert(Long organizerId, Long concertId, String reason) {
        Concert concert = getConcertDetail(concertId);
        verifyOrganizationMembership(organizerId, concert);
        transitionApproval(concert, ConcertApprovalStatus.REJECTED);
        concert.setReviewedBy(userRepository.getReferenceById(organizerId));
        concert.setReviewedAt(OffsetDateTime.now());
        concert.setRejectionReason(reason);
        return concertRepository.save(concert);
    }


    /**
     * Explicit publish action: UPCOMING -> ON_SALE.
     */
    @CacheEvict(value = {"concerts", "concertDetail"}, allEntries = true)
    @Transactional
    public Concert publishConcert(Long concertId) {
        Concert concert = getConcertDetail(concertId);

        if (concert.getApprovalStatus() != ConcertApprovalStatus.APPROVED) {
            throw new InvalidApprovalTransitionException(
                    "Cannot publish a concert that has not been APPROVED by its organizer. Current approval status: "
                            + concert.getApprovalStatus());
        }
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

    private void transitionApproval(Concert concert, ConcertApprovalStatus target) {
        if (!concert.getApprovalStatus().canTransitionTo(target)) {
            throw new InvalidApprovalTransitionException(String.format(
                    "Cannot transition concert %d approval status from %s to %s",
                    concert.getId(), concert.getApprovalStatus(), target));
        }
        concert.setApprovalStatus(target);
    }

    private final OrganizationMembershipChecker membershipChecker;

    private void verifyOrganizationMembership(Long userId, Concert concert) {
        if (concert.getOrganization() == null
                || !membershipChecker.isMember(userId, concert.getOrganization().getId())) {
            throw new ResourceNotFoundException("Concert not found: " + concert.getId());
        }
    }
}