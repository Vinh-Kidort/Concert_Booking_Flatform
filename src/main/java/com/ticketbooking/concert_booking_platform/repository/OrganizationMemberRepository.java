package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {
    List<OrganizationMember> findByUserId(Long userId);
    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long organizationId, Long userId);
    List<OrganizationMember> findByOrganizationId(Long organizationId);
}