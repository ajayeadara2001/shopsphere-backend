package com.shopsphere.service.serviceImpl;

import com.shopsphere.dto.ApplyCouponRequestDto;
import com.shopsphere.dto.ApplyCouponResponseDto;
import com.shopsphere.dto.CouponRequestDto;
import com.shopsphere.dto.CouponResponseDto;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.Coupon;
import com.shopsphere.entity.User;
import com.shopsphere.exception.CartNotFoundException;
import com.shopsphere.exception.CouponException;
import com.shopsphere.exception.CouponNotFoundException;
import com.shopsphere.mapper.CartMapper;
import com.shopsphere.mapper.CouponMapper;
import com.shopsphere.mapper.ProductMapper;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.CouponRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {
    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;
    private final CouponMapper couponMapper;
    private final UserMapper userMapper;

    private String normalizeCouponCode(String couponCode) {
        return couponCode.trim().toUpperCase(Locale.ROOT);
    }
    @Override
    @Transactional
    public CouponResponseDto createCoupon(CouponRequestDto couponRequestDto) {
        String couponCode = normalizeCouponCode(couponRequestDto.getCouponCode());
        if (couponRepository.existsByCouponCode(couponCode)) {
            throw new CouponException("Coupon code already exists.");
        }
        Coupon newCoupon=new Coupon();
        newCoupon.setCouponCode(couponCode);
        newCoupon.setDiscountPercentage(couponRequestDto.getDiscountPercentage());
        newCoupon.setMinimumOrderAmount(couponRequestDto.getMinimumOrderAmount());
        newCoupon.setExpiryDate(couponRequestDto.getExpiryDate());
        newCoupon.setActiveStatus(couponRequestDto.getActiveStatus());
        newCoupon.setUsageLimit(couponRequestDto.getUsageLimit());
        newCoupon.setUsageCount(0);
        return couponMapper.mapCouponToResponse(couponRepository.save(newCoupon));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponseDto getCouponById(Long couponId) {
        return couponMapper.mapCouponToResponse
                (couponMapper.getCouponById(couponId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponseDto> getAllCoupons() {
        List<Coupon> couponList=couponRepository.findAll();
        List<CouponResponseDto>couponResponseDtoList=new ArrayList<>();
        for(Coupon coupon:couponList){
            couponResponseDtoList.add(couponMapper.mapCouponToResponse(coupon));
        }
        return couponResponseDtoList;
    }

    @Override
    public List<CouponResponseDto> getAllActiveCoupons() {
        List<Coupon> couponList=couponRepository.findAllByActiveStatusTrue();
        List<CouponResponseDto>couponResponseDtoList=new ArrayList<>();
        for(Coupon coupon:couponList){
            couponResponseDtoList.add(couponMapper.mapCouponToResponse(coupon));
        }
        return couponResponseDtoList;
    }

    @Override
    @Transactional
    public CouponResponseDto updateCoupon(Long couponId,CouponRequestDto couponRequestDto) {
        Coupon existingCoupon=couponMapper.getCouponById(couponId);
        if(couponRequestDto.getDiscountPercentage()!=null){
            existingCoupon.setDiscountPercentage(couponRequestDto.getDiscountPercentage());
        }
        if(couponRequestDto.getMinimumOrderAmount()!=null){
            existingCoupon.setMinimumOrderAmount(couponRequestDto.getMinimumOrderAmount());
        }
        if(couponRequestDto.getExpiryDate()!=null){
            existingCoupon.setExpiryDate(couponRequestDto.getExpiryDate());
        }
        if(couponRequestDto.getUsageLimit()!=null){
            existingCoupon.setUsageLimit(couponRequestDto.getUsageLimit());
        }
        if(couponRequestDto.getActiveStatus()!=null){
            existingCoupon.setActiveStatus(couponRequestDto.getActiveStatus());
        }
        return couponMapper.mapCouponToResponse
                (couponRepository.save(existingCoupon));
    }

    @Override
    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon existingCoupon=couponMapper.getCouponById(couponId);
        existingCoupon.setActiveStatus(false);
        couponRepository.save(existingCoupon);
    }

    @Override
    @Transactional
    public ApplyCouponResponseDto applyCoupon(ApplyCouponRequestDto applyCouponRequestDto) {
        String couponCode = normalizeCouponCode(applyCouponRequestDto.getCouponCode());
        Coupon coupon=couponRepository.findByCouponCode(couponCode)
                .orElseThrow(()-> new CouponNotFoundException("Invalid Coupon code"));
        if(coupon.getUsageLimit()!=null
                && coupon.getUsageCount() >= coupon.getUsageLimit()){
            throw new CouponException("Coupon usage limit exceeded.");
        }
        if(coupon.getExpiryDate() == null
                || !coupon.getExpiryDate().isAfter(java.time.LocalDateTime.now())){
            throw new CouponException("Coupon is expired.");
        }
        if(!Boolean.TRUE.equals(coupon.getActiveStatus())){
            throw new CouponException("Coupon is inactive.");
        }
        User currentUser=userMapper.getCurrentUser();
        Cart cart=cartRepository.findByUser(currentUser)
                .orElseThrow(()->new CartNotFoundException("Cart not found for user."));
        BigDecimal cartTotal=cart.getTotalAmount();
        if(cartTotal.compareTo(coupon.getMinimumOrderAmount())<0){
            throw new CouponException
                    ("Minimum order amount is "+coupon.getMinimumOrderAmount());
        }
        BigDecimal discount=cartTotal
                .multiply(coupon.getDiscountPercentage())
                .divide(BigDecimal.valueOf(100),2, RoundingMode.HALF_UP);
        BigDecimal finalAmount=cartTotal.subtract(discount);
        return ApplyCouponResponseDto.builder()
                .couponCode(coupon.getCouponCode())
                .discount(discount)
                .finalAmount(finalAmount)
                .cartTotal(cartTotal)
                .message("Coupon applied successfully")
                .build();

    }
}
