package com.shopsphere.mapper;

import com.shopsphere.dto.CouponResponseDto;
import com.shopsphere.entity.Coupon;
import com.shopsphere.exception.CartNotFoundException;
import com.shopsphere.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponMapper {
    private final CouponRepository couponRepository;
    public CouponResponseDto mapCouponToResponse(Coupon coupon){
        return new CouponResponseDto(
                coupon.getCouponId(),
                coupon.getCouponCode(),
                coupon.getDiscountPercentage(),
                coupon.getMinimumOrderAmount(),
                coupon.getExpiryDate(),
                coupon.getActiveStatus(),
                coupon.getUsageLimit(),
                coupon.getUsageCount()
        );
    }
    public Coupon getCouponById(Long couponId){
        return couponRepository.findById(couponId)
                .orElseThrow(()-> new CartNotFoundException
                        ("Coupon not found for ID: "+couponId));
    }
}
