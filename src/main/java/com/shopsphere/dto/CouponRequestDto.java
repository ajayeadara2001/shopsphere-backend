package com.shopsphere.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class CouponRequestDto {
    @NotBlank(groups = OnCreate.class, message = "Coupon code cannot be blank")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$",
            message = "Coupon code can contain only letters, numbers, underscores and hyphens")
    private String couponCode;

    @NotNull(groups = OnCreate.class, message = "Discount percentage is required")
    @DecimalMin(value = "0.01", message = "Discount must be greater than 0")
    @DecimalMax(value = "100.00", message = "Discount cannot exceed 100%")
    private BigDecimal discountPercentage;

    @NotNull(groups = OnCreate.class, message = "Minimum order amount is required")
    @PositiveOrZero(message = "Minimum order amount cannot be negative")
    private BigDecimal minimumOrderAmount;

    @NotNull(groups = OnCreate.class, message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiryDate;

    @NotNull(groups = OnCreate.class, message = "Active Status is required")
    private Boolean activeStatus;

    @NotNull(groups = OnCreate.class, message = "Usage limit is required")
    @Min(value = 1, message = "Usage limit must be at least 1")
    @Max(value = 1000000, message = "Usage limit is too large")
    private Integer usageLimit;
}
