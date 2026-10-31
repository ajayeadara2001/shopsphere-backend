package com.shopsphere.controller;

import com.shopsphere.dto.OnCreate;
import com.shopsphere.dto.WishlistRequestDto;
import com.shopsphere.dto.WishlistResponseDto;
import com.shopsphere.service.WishlistService;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class WishlistController {
    private final WishlistService wishlistService;

    @PostMapping
    public ResponseEntity<WishlistResponseDto> addToWishlist(
            @Validated({Default.class, OnCreate.class})
            @RequestBody WishlistRequestDto wishlistRequestDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                wishlistService.addToWishlist(wishlistRequestDto)
        );
    }

    @GetMapping("/{wishlistId}")
    public ResponseEntity<WishlistResponseDto> getWishlistById(
            @PathVariable Long wishlistId){
        return ResponseEntity.ok(wishlistService.getWishlistById(wishlistId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<WishlistResponseDto>> getUserAllWishlists(){
        return ResponseEntity.ok(wishlistService.getUserAllWishlists());
    }

    @DeleteMapping("/{wishlistId}")
    public ResponseEntity<WishlistResponseDto> deleteWishlist(
            @PathVariable Long wishlistId){
        wishlistService.deleteWishlist(wishlistId);
        return ResponseEntity.noContent().build();
    }
}
