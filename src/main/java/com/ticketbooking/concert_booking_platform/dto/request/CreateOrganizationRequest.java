package com.ticketbooking.concert_booking_platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    private String name;

    @NotNull(message = "Initial owner user ID is required")
    private Long initialOwnerUserId;
}