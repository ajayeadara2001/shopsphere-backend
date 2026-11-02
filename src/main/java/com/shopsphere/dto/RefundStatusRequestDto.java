package com.shopsphere.dto;

import com.shopsphere.constant.RefundStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundStatusRequestDto {
    @NotNull(message = "Refund status is required.")
    private RefundStatus refundStatus;
}
