package com.shopsphere.dto;

import com.shopsphere.constant.RefundStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundStatusRequestDto {
    @NotBlank(message = "Refund Status cannot be blank.")
    private RefundStatus refundStatus;
}
