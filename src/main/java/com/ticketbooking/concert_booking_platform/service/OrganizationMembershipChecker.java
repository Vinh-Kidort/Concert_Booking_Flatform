package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.repository.OrganizationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationMembershipChecker {

    private final OrganizationMemberRepository organizationMemberRepository;

    public boolean isMember(Long userId, Long organizationId) {
        if (organizationId == null) return false;
        return organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userId).isPresent();
    }

    public boolean isOwner(Long userId, Long organizationId) {
        return organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .map(m -> "OWNER".equals(m.getMemberRole()))
                .orElse(false);
    }
}