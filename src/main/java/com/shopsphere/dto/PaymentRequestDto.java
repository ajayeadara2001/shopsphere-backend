package com.shopsphere.dto;

import com.shopsphere.constant.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentRequestDto {
    @NotNull(message= "Payment Method is required")
    private PaymentMethod paymentMethod;
}
