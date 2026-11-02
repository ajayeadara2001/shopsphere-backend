package com.shopsphere.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WishlistRequestDto {
    @NotNull(message = "Product Id is required.")
    @Positive(message = "Product Id must be positive.")
    private Long productId;
}
