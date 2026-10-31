package com.shopsphere.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponseDto {
    private Long couponId;
    private String couponCode;
    private BigDecimal discountPercentage;
    private BigDecimal minimumOrderAmount;
    private LocalDateTime expiryDate;
    private Boolean activeStatus;
    private Integer usageLimit;
    private Integer usageCount;
}
