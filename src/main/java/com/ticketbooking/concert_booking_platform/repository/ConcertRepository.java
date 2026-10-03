package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.enums.ConcertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    Page<Concert> findByStatus(ConcertStatus status, Pageable pageable);


    @Query("SELECT c FROM Concert c WHERE c.organization.id IN :organizationIds")
    Page<Concert> findByOrganizationIdIn(List<Long> organizationId, Pageable pageable);
}