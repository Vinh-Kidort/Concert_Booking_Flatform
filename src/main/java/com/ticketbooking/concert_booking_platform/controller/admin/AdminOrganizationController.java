package com.ticketbooking.concert_booking_platform.controller.admin;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.CreateOrganizationRequest;
import com.ticketbooking.concert_booking_platform.entity.Organization;
import com.ticketbooking.concert_booking_platform.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/organizations")
@RequiredArgsConstructor
public class AdminOrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ApiResponse<Organization> create(@Valid @RequestBody CreateOrganizationRequest request) {
        return ApiResponse.success(
                organizationService.createOrganization(request.getName(), request.getInitialOwnerUserId()));
    }
}