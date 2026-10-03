package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.entity.Organization;
import com.ticketbooking.concert_booking_platform.entity.OrganizationMember;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.OrganizationMemberRepository;
import com.ticketbooking.concert_booking_platform.repository.OrganizationRepository;
import com.ticketbooking.concert_booking_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipChecker membershipChecker;

    /** Admin-only onboarding action — creates a new tenant and its first OWNER. */
    @Transactional
    public Organization createOrganization(String name, Long initialOwnerUserId) {
        userRepository.findById(initialOwnerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + initialOwnerUserId));

        Organization org = organizationRepository.save(Organization.builder().name(name).build());

        organizationMemberRepository.save(OrganizationMember.builder()
                .organizationId(org.getId())
                .userId(initialOwnerUserId)
                .memberRole("OWNER")
                .build());

        return org;
    }

    /**
     * Only an existing OWNER of the organization may add staff — keeps
     * tenant membership changes self-service for the organizer's own team
     * without requiring Admin involvement for every new hire, while still
     * requiring someone already inside the tenant to vouch for the addition.
     */
    @Transactional
    public void addStaffMember(Long requestingUserId, Long organizationId, Long newMemberUserId) {
        if (!membershipChecker.isOwner(requestingUserId, organizationId)) {
            throw new ResourceNotFoundException("Organization not found: " + organizationId);
        }

        userRepository.findById(newMemberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + newMemberUserId));

        if (membershipChecker.isMember(newMemberUserId, organizationId)) {
            throw new IllegalStateException("User is already a member of this organization");
        }

        organizationMemberRepository.save(OrganizationMember.builder()
                .organizationId(organizationId)
                .userId(newMemberUserId)
                .memberRole("STAFF")
                .build());
    }

    @Transactional
    public void removeMember(Long requestingUserId, Long organizationId, Long targetUserId) {
        if (!membershipChecker.isOwner(requestingUserId, organizationId)) {
            throw new ResourceNotFoundException("Organization not found: " + organizationId);
        }
        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(organizationId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if ("OWNER".equals(member.getMemberRole())) {
            throw new IllegalStateException("Cannot remove the organization owner");
        }
        organizationMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public List<OrganizationMember> listMembers(Long requestingUserId, Long organizationId) {
        if (!membershipChecker.isMember(requestingUserId, organizationId)) {
            throw new ResourceNotFoundException("Organization not found: " + organizationId);
        }
        return organizationMemberRepository.findByOrganizationId(organizationId);
    }
}