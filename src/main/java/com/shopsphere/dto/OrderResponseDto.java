package com.shopsphere.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shopsphere.constant.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponseDto {
    private Long orderId;
    private String orderNumber;
    private BigDecimal totalOrderAmount;
    private OrderStatus orderStatus;
    private List<OrderItemsResponseDto> orderItemsList;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderedAt;
}
