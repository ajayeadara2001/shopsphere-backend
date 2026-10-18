package com.shopsphere.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductRequestDto {
        @NotBlank(groups = OnCreate.class, message = "Product name cannot be blank")
        @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
        private String productName;
        @NotBlank(groups = OnCreate.class, message = "Description cannot be blank")
        @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
        private String description;
        @NotNull(groups = OnCreate.class, message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "Price can have up to 10 digits and 2 decimal places")
        private BigDecimal price;
        @NotNull(groups = OnCreate.class, message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        @Max(value = 1000000, message = "Stock quantity is too large")
        private Integer stockQuantity;
        @NotBlank(groups = OnCreate.class, message = "Brand cannot be blank")
        @Size(min = 2, max = 50, message = "Brand must be between 2 and 50 characters")
        private String brand;
        @NotBlank(groups = OnCreate.class, message = "Image URL cannot be blank")
        private String imageUrl;
        @NotNull(groups = OnCreate.class, message = "Category id is required")
        @Positive(message = "Category id must be a positive number")
        private Long categoryId;
    }

