package com.shopsphere.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemsResponseDto {
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal priceAtAdded;

}
