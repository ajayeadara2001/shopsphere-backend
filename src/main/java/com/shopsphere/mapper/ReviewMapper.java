package com.shopsphere.mapper;

import com.shopsphere.dto.ReviewResponseDto;
import com.shopsphere.entity.Review;
import com.shopsphere.exception.ReviewNotFoundException;
import com.shopsphere.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewMapper {
    private final ReviewRepository reviewRepository;
    public ReviewResponseDto mapReviewToResponse(Review review){
        return new ReviewResponseDto(
                review.getReviewId(),
                review.getReviewComments(),
                review.getRating(),
                review.getUser().getUserId(),
                review.getUser().getUserName(),
                review.getProduct().getProductId(),
                review.getProduct().getProductName(),
                review.getReviewAt()
        );
    }

    public Review getReviewById(Long reviewId){
        return reviewRepository.findById(reviewId)
                .orElseThrow(()-> new ReviewNotFoundException("Review not found for Id: "+reviewId));
    }
}
