package com.shopsphere.dto;

import com.shopsphere.constant.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderStatusUpdateRequestDto {
    @NotNull(message = "Order Status is required.")
    private OrderStatus orderStatus;
}
