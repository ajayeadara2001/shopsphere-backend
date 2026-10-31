package com.shopsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApplyCouponRequestDto {
    @NotBlank(groups = OnCreate.class, message = "Coupon code cannot be blank")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$",
            message = "Coupon code can contain only letters, numbers, underscores and hyphens")
    private String couponCode;
}
