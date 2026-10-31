package com.shopsphere.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto {
    private Long reviewId;
    private String reviewComments;
    private Integer rating;
    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
    @JsonFormat(pattern = "yyyy-MM-dd HH-mm-ss")
    private LocalDateTime reviewAt;
}
