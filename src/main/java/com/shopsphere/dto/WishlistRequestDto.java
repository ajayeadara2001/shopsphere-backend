package com.shopsphere.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WishlistRequestDto {
    @NotNull(message = "Product Id is required.")
    private Long productId;
}
