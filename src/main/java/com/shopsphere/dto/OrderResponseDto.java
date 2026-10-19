package com.shopsphere.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shopsphere.constant.OrderStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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
