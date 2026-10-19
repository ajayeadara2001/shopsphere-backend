package com.shopsphere.controller;

import com.shopsphere.dto.CartItemsRequestDto;
import com.shopsphere.dto.CartResponseDto;
import com.shopsphere.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<CartResponseDto> createCart(
            @Valid @RequestBody CartItemsRequestDto cartItemsRequestDto){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cartService.createCart(cartItemsRequestDto));
    }
    @GetMapping
    public ResponseEntity<CartResponseDto> getCart(){
        return ResponseEntity.ok(cartService.getCart());
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponseDto> updateCart(
            @PathVariable Long productId, @RequestParam Integer quantity){
        return ResponseEntity.ok(cartService.updateCartItems(productId,quantity));
    }

    @DeleteMapping
    public ResponseEntity<String> clearCart(){
        cartService.clearCart();
        return ResponseEntity.ok("Cart cleared successfully");
    }
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<String> removeCartItem(@PathVariable Long productId){
        cartService.removeCartItem(productId);
        return ResponseEntity.ok("Items removed from cart successfully");
    }


}
