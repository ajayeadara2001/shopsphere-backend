package com.shopsphere.service;

import com.shopsphere.dto.ApplyCouponRequestDto;
import com.shopsphere.dto.ApplyCouponResponseDto;
import com.shopsphere.dto.CouponRequestDto;
import com.shopsphere.dto.CouponResponseDto;

import java.util.List;

public interface CouponService {
    CouponResponseDto createCoupon(CouponRequestDto couponRequestDto);
    CouponResponseDto getCouponById(Long couponId);
    List<CouponResponseDto> getAllCoupons();
    List<CouponResponseDto> getAllActiveCoupons();
    CouponResponseDto updateCoupon(Long couponId,CouponRequestDto couponRequestDto);
    void deleteCoupon(Long couponId);
    ApplyCouponResponseDto applyCoupon(ApplyCouponRequestDto applyCouponRequestDto);
}
