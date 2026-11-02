package com.shopsphere.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDto {
    @NotNull(message = "Product Id is required.")
    @Positive(message = "Product Id must be positive.")
    private Long productId;
    @NotBlank(groups = OnCreate.class, message = "Review comments cannot be blank")
    @Size(min = 5, max = 1000, message = "Review comments must be between 5 and 1000 characters")
    private String reviewComments;

    @NotNull(groups = OnCreate.class, message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;
}
