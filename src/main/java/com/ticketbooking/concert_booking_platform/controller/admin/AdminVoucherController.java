package com.ticketbooking.concert_booking_platform.controller.admin;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.CreateVoucherRequest;
import com.ticketbooking.concert_booking_platform.dto.response.VoucherResponse;
import com.ticketbooking.concert_booking_platform.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
@Tag(name = "7. Operation Dashboard - Vouchers", description = "Manage promotional voucher campaigns and monitor quota usage (OPERATOR / ADMIN roles)")
@SecurityRequirement(name = "bearerAuth")
public class AdminVoucherController {

    private final VoucherService voucherService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new voucher campaign", description = "Publish a new promotional voucher code with global usage limits and validity dates.")
    public ApiResponse<VoucherResponse> createVoucher(@Valid @RequestBody CreateVoucherRequest request) {
        VoucherResponse response = voucherService.createVoucher(request);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(summary = "List all vouchers", description = "Monitor active and past voucher campaigns and inspect remaining quota.")
    public ApiResponse<Page<VoucherResponse>> getVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<VoucherResponse> response = voucherService.getAllVouchers(PageRequest.of(page, size));
        return ApiResponse.success(response);
    }
}