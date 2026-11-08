package com.shopsphere.controller;

import com.shopsphere.dto.CartItemsRequestDto;
import com.shopsphere.dto.CartResponseDto;
import com.shopsphere.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
@Validated
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
            @PathVariable Long productId, @RequestParam @Min(1) @Max(100) Integer quantity){
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