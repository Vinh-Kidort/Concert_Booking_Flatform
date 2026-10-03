package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}