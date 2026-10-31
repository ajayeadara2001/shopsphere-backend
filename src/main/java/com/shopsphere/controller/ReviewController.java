package com.shopsphere.controller;

import com.shopsphere.dto.*;
import com.shopsphere.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor

public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/{productId}")
    public ResponseEntity<ReviewResponseDto> addReview(
            @Validated({Default.class, OnCreate.class})
            @RequestBody ReviewRequestDto reviewRequestDto,
            @PathVariable Long productId){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.addReview(productId, reviewRequestDto));

    }
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> getReviewById(
            @PathVariable Long reviewId){
        return ResponseEntity.ok(reviewService.getReviewById(reviewId));
    }
    @GetMapping("/all/{productId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByProductId(
            @PathVariable Long productId){
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequestDto reviewRequestDto){
        return ResponseEntity
                .ok(reviewService.updateReview(reviewId,reviewRequestDto));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId){
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
