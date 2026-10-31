package com.shopsphere.mapper;

import com.shopsphere.dto.WishlistResponseDto;
import com.shopsphere.entity.Wishlist;
import org.springframework.stereotype.Component;

@Component
public class WishlistMapper {
    public WishlistResponseDto mapWishlistToResponse(Wishlist wishlist){
        return new WishlistResponseDto(
                wishlist.getWishlistId(),
                wishlist.getProduct().getProductId(),
                wishlist.getProduct().getProductName(),
                wishlist.getProduct().getPrice(),
                wishlist.getProduct().getDescription(),
                wishlist.getProduct().getImageUrl(),
                wishlist.getAddedAt()
        );
    }
}
