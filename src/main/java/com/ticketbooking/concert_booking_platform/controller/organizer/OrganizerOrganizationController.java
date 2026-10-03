package com.ticketbooking.concert_booking_platform.controller.organizer;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.entity.OrganizationMember;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// OrganizerOrganizationController.java — OWNER tự quản lý thành viên
@RestController
@RequestMapping("/api/v1/organizer/organizations/{organizationId}/members")
@RequiredArgsConstructor
public class OrganizerOrganizationController {

    private final OrganizationService organizationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ApiResponse<Void> addMember(@PathVariable Long organizationId, @RequestParam Long userId) {
        organizationService.addStaffMember(currentUserProvider.getCurrentUserId(), organizationId, userId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long organizationId, @PathVariable Long userId) {
        organizationService.removeMember(currentUserProvider.getCurrentUserId(), organizationId, userId);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<OrganizationMember>> list(@PathVariable Long organizationId) {
        return ApiResponse.success(
                organizationService.listMembers(currentUserProvider.getCurrentUserId(), organizationId));
    }
}