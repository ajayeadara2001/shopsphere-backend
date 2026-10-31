package com.shopsphere.controller.adminContoller;

import com.shopsphere.dto.CouponRequestDto;
import com.shopsphere.dto.CouponResponseDto;
import com.shopsphere.dto.OnCreate;
import com.shopsphere.service.CouponService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCouponController {
    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponseDto> createCoupon(
           @Validated({Default.class, OnCreate.class})
           @RequestBody CouponRequestDto couponRequestDto){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(couponService.createCoupon(couponRequestDto));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<CouponResponseDto> getCouponById(
            @PathVariable Long couponId){
        return ResponseEntity.ok(couponService.getCouponById(couponId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CouponResponseDto>> getAllCoupons(){
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @GetMapping("/all/active")
    public ResponseEntity<List<CouponResponseDto>> getAllActiveCoupons(){
        return ResponseEntity.ok(couponService.getAllActiveCoupons());
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<CouponResponseDto> updateCoupon(
            @Valid @RequestBody CouponRequestDto couponRequestDto,
            @PathVariable Long couponId){
        return ResponseEntity.ok(couponService.updateCoupon(couponId,couponRequestDto));
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> deleteCoupon(
            @PathVariable Long couponId){
        couponService.deleteCoupon(couponId);
        return ResponseEntity.noContent().build();
    }
}
