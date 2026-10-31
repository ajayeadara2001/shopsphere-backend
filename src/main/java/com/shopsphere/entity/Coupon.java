package com.shopsphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;
    @Column(nullable = false,unique = true)
    private String couponCode;
    private BigDecimal discountPercentage;
    private BigDecimal minimumOrderAmount;
    private LocalDateTime expiryDate;
    private Boolean activeStatus;
    private Integer usageLimit;
    private Integer usageCount;
}
