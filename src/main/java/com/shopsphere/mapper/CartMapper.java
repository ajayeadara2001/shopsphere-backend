package com.shopsphere.mapper;

import com.shopsphere.dto.CartItemsResponseDto;
import com.shopsphere.dto.CartResponseDto;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.CartItems;
import com.shopsphere.entity.User;
import com.shopsphere.exception.CartNotFoundException;
import com.shopsphere.repository.CartItemsRepository;
import com.shopsphere.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CartMapper {
    private final CartRepository cartRepository;
    private final CartItemsRepository cartItemsRepository;
    public CartResponseDto mapCartToResponse(Cart cart){
        List<CartItems> cartItemsList=cartItemsRepository.findByCart(cart);
        CartResponseDto cartResponseDto=new CartResponseDto();
        cartResponseDto.setCartId(cart.getCartId());
        List<CartItemsResponseDto> cartItemsResponseDtoList=new ArrayList<>();
        for(CartItems cartItems:cartItemsList){
            CartItemsResponseDto cartItemsResponseDto=new CartItemsResponseDto();
            cartItemsResponseDto.setProductId(cartItems.getProduct().getProductId());
            cartItemsResponseDto.setProductName(cartItems.getProduct().getProductName());
            cartItemsResponseDto.setProductPrice(cartItems.getPriceAtAdded());
            cartItemsResponseDto.setQuantity(cartItems.getQuantity());
            cartItemsResponseDto.setSubTotal(cartItems.getPriceAtAdded()
                    .multiply(BigDecimal.valueOf(cartItems.getQuantity())));
            cartItemsResponseDtoList.add(cartItemsResponseDto);
        }
        cartResponseDto.setCartItemsList(cartItemsResponseDtoList);
        cartResponseDto.setTotalAmount(cart.getTotalAmount());
        cartResponseDto.setCreatedAt(cart.getCreatedAt());
        cartResponseDto.setUpdatedAt(cart.getUpdatedAt());
        return cartResponseDto;
    }

    public Cart getCartForUser(User user){
        return cartRepository.findByUser(user)
                .orElseThrow(()-> new CartNotFoundException("Cart not found"));
    }

    public void calculateTotalCartAmount(Cart cart){
        List<CartItems> cartItemsList=cartItemsRepository.findByCart(cart);
        BigDecimal totalAmount=BigDecimal.ZERO;
        for(CartItems cartItems:cartItemsList){
            BigDecimal subTotal= cartItems.getPriceAtAdded()
                    .multiply(BigDecimal.valueOf(cartItems.getQuantity()));
            totalAmount=totalAmount.add(subTotal);
        }
        cart.setTotalAmount(totalAmount);
        cartRepository.save(cart);
    }
}
