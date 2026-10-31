package com.shopsphere.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplyCouponResponseDto {
    private String couponCode;
    private BigDecimal cartTotal;
    private BigDecimal discount;
    private BigDecimal finalAmount;
    private String message;
}
