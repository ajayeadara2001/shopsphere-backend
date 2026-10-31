package com.shopsphere.service;

import com.shopsphere.dto.ReviewRequestDto;
import com.shopsphere.dto.ReviewResponseDto;

import java.util.List;

public interface ReviewService {
    ReviewResponseDto addReview(Long productId,ReviewRequestDto reviewRequestDto);
    ReviewResponseDto getReviewById(Long reviewId);
    List<ReviewResponseDto> getReviewsByProductId(Long productId);
    ReviewResponseDto updateReview(Long reviewId,ReviewRequestDto reviewRequestDto);
    void deleteReview(Long reviewId);
}
