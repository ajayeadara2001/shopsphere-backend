package com.shopsphere.controller;


import com.shopsphere.dto.ApplyCouponRequestDto;
import com.shopsphere.dto.ApplyCouponResponseDto;
import com.shopsphere.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @PostMapping("/apply-coupon")
    public ResponseEntity<ApplyCouponResponseDto> applyCoupon(
            @Valid @RequestBody ApplyCouponRequestDto applyCouponRequestDto){
        return ResponseEntity.ok(couponService.applyCoupon(applyCouponRequestDto));
    }
}
