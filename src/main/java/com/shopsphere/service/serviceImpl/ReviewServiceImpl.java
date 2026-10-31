package com.shopsphere.service.serviceImpl;

import com.shopsphere.constant.Role;
import com.shopsphere.dto.ReviewRequestDto;
import com.shopsphere.dto.ReviewResponseDto;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.Review;
import com.shopsphere.entity.User;
import com.shopsphere.exception.ReviewException;
import com.shopsphere.mapper.ProductMapper;
import com.shopsphere.mapper.ReviewMapper;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.OrderRepository;
import com.shopsphere.repository.ReviewRepository;
import com.shopsphere.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final ReviewMapper reviewMapper;
    @Override
    @Transactional
    public ReviewResponseDto addReview(Long productId, ReviewRequestDto reviewRequestDto) {
        User currentUser=userMapper.getCurrentUser();
        Product product=productMapper.getProduct(productId);
        if(reviewRepository.findByUserAndProduct(currentUser,product).isPresent()){
            throw new ReviewException("You have already reviewed this product.");
        }
        boolean hasPurchased=orderRepository
                .existsByUserAndOrderItemsListProduct(currentUser,product);
        if(!hasPurchased){
            throw new ReviewException("You can only review the products you have purchased.");
        }
        Review newReview=new Review();
        newReview.setRating(reviewRequestDto.getRating());
        newReview.setReviewComments(reviewRequestDto.getReviewComments());
        newReview.setUser(currentUser);
        newReview.setProduct(product);
        newReview.setReviewAt(LocalDateTime.now());
        return reviewMapper.mapReviewToResponse(reviewRepository.save(newReview));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getReviewById(Long reviewId) {
        return reviewMapper.mapReviewToResponse
                (reviewMapper.getReviewById(reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByProductId(Long productId) {
        List<Review> reviewList=reviewRepository.findByProduct
                (productMapper.getProduct(productId));
        List<ReviewResponseDto> reviewResponseDtoList=new ArrayList<>();
        for(Review review:reviewList){
            reviewResponseDtoList.add(reviewMapper.mapReviewToResponse(review));
        }
        return reviewResponseDtoList;
    }

    @Override
    @Transactional
    public ReviewResponseDto updateReview(Long reviewId, ReviewRequestDto reviewRequestDto) {
        User currentUser=userMapper.getCurrentUser();
        Review review=reviewMapper.getReviewById(reviewId);
        boolean isOwner=review.getUser().getUserId().equals(currentUser.getUserId());
        boolean isAdmin=currentUser.getRole()== Role.ADMIN;
        if(!isAdmin && !isOwner){
            throw new ReviewException("You can only update your own review.");
        }
        review.setRating(reviewRequestDto.getRating());
        review.setReviewComments(reviewRequestDto.getReviewComments());
        review.setUser(currentUser);
        review.setReviewAt(LocalDateTime.now());
        return reviewMapper.mapReviewToResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        User currentUser=userMapper.getCurrentUser();
        Review review=reviewMapper.getReviewById(reviewId);
        boolean isOwner=review.getUser().getUserId().equals(currentUser.getUserId());
        boolean isAdmin=currentUser.getRole()== Role.ADMIN;
        if(!isAdmin && !isOwner){
            throw new ReviewException("You can only delete your own review.");
        }
        reviewRepository.delete(review);
    }
}
