package com.shopsphere.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WishlistResponseDto {
    private Long wishlistId;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private String productDescription;
    private String imageUrl;
    private LocalDateTime addedAt;
}
