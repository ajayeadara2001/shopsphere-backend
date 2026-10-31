package com.shopsphere.service;

import com.shopsphere.dto.WishlistRequestDto;
import com.shopsphere.dto.WishlistResponseDto;

import java.util.List;

public interface WishlistService {
    WishlistResponseDto addToWishlist(WishlistRequestDto wishlistRequestDto);
    List<WishlistResponseDto> getUserAllWishlists();
    WishlistResponseDto getWishlistById(Long wishlistId);
    void deleteWishlist(Long wishlistId);
}
