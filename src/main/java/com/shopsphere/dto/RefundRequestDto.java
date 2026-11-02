package com.shopsphere.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RefundRequestDto {
    @NotNull(message = "Payment id is required")
    @Positive(message = "Payment Id must be positive.")
    private Long paymentId;
    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    private BigDecimal refundAmount;
    @NotBlank(message = "Refund reason cannot be blank")
    @Size(min = 5, max = 300, message = "Refund reason must be between 5 and 300 characters")
    private String refundReason;
}
