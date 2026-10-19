package com.shopsphere.service;

import com.shopsphere.dto.CartItemsRequestDto;
import com.shopsphere.dto.CartResponseDto;

public interface CartService {
    CartResponseDto createCart(CartItemsRequestDto cartItemsRequestDto);
    CartResponseDto getCart();
    CartResponseDto updateCartItems(Long productId, Integer quantity);
    void clearCart();
    void removeCartItem(Long productId);
}
