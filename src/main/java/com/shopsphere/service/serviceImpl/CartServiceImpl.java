package com.shopsphere.service.serviceImpl;

import com.shopsphere.dto.CartItemsRequestDto;
import com.shopsphere.dto.CartResponseDto;
import com.shopsphere.entity.Cart;
import com.shopsphere.entity.CartItems;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import com.shopsphere.exception.CartItemsNotFoundException;
import com.shopsphere.mapper.CartMapper;
import com.shopsphere.mapper.ProductMapper;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.CartItemsRepository;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemsRepository cartItemsRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;


    @Override
    @Transactional
    public CartResponseDto createCart(CartItemsRequestDto cartItemsRequestDto) {
        User existingUser=userMapper.getCurrentUser();
        Cart cart=cartRepository.findByUser(existingUser)
                .orElseGet(()-> {
                            Cart newCart = new Cart();
                            newCart.setUser(existingUser);
                            newCart.setTotalAmount(BigDecimal.ZERO);
                            return cartRepository.save(newCart);
                        });
        Product existingProduct=productMapper.getProduct(cartItemsRequestDto.getProductId());
        if(!existingProduct.getActive()){
            throw new IllegalArgumentException("Product is not available.");
        }
        if(cartItemsRequestDto.getQuantity() > existingProduct.getStockQuantity()){
            throw new IllegalArgumentException("Only "+existingProduct.getStockQuantity()+" is available.");
        }
        Optional<CartItems> optionalCartItems=cartItemsRepository
                .findByCartAndProduct(cart,existingProduct);
        CartItems cartItems;
        if(optionalCartItems.isPresent()){
            cartItems=optionalCartItems.get();
            int updateQuantity= cartItems.getQuantity() + cartItemsRequestDto.getQuantity();
            if(updateQuantity> existingProduct.getStockQuantity()){
                throw new IllegalArgumentException("Only "+existingProduct.getStockQuantity()+" is available.");
            }
            cartItems.setQuantity(updateQuantity);
        }else{
            cartItems=new CartItems();
            cartItems.setCart(cart);
            cartItems.setProduct(existingProduct);
            cartItems.setQuantity(cartItemsRequestDto.getQuantity());
            cartItems.setPriceAtAdded(existingProduct.getPrice());
        }
        cartItemsRepository.save(cartItems);
        cartMapper.calculateTotalCartAmount(cart);
        cartRepository.save(cart);
        return cartMapper.mapCartToResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponseDto getCart() {
        User existingUser=userMapper.getCurrentUser();
        Cart existingCart=cartMapper.getCartForUser(existingUser);
        return cartMapper.mapCartToResponse(existingCart);
    }

    @Override
    @Transactional
    public CartResponseDto updateCartItems(Long productId, Integer quantity) {
        User existingUser=userMapper.getCurrentUser();
        Cart existingCart=cartMapper.getCartForUser(existingUser);
        Product existingProduct=productMapper.getProduct(productId);
        CartItems cartItems=cartItemsRepository.findByCartAndProduct(existingCart,existingProduct)
                .orElseThrow(()-> new CartItemsNotFoundException("Cart items not found"));
        if(quantity<=0){
            throw new IllegalArgumentException("Quantity should be greater then zero.");
        }
        if(quantity>existingProduct.getStockQuantity()){
            throw new IllegalArgumentException("Only "+existingProduct.getStockQuantity()+" is available.");
        }
        cartItems.setQuantity(quantity);
        cartItems.setPriceAtAdded(existingProduct.getPrice());
        cartItemsRepository.save(cartItems);
        cartMapper.calculateTotalCartAmount(existingCart);
        return cartMapper.mapCartToResponse(cartRepository.save(existingCart));
    }

    @Override
    @Transactional
    public void clearCart() {
        User existingUser=userMapper.getCurrentUser();
        Cart existingCart=cartMapper.getCartForUser(existingUser);
        List<CartItems> cartItemsList=cartItemsRepository.findByCart(existingCart);
        if(!cartItemsList.isEmpty()){
            cartItemsRepository.deleteAll(cartItemsList);
        }
        cartMapper.calculateTotalCartAmount(existingCart);
    }

    @Override
    @Transactional
    public void removeCartItem(Long productId) {
        User existingUser=userMapper.getCurrentUser();
        Cart existingCart=cartMapper.getCartForUser(existingUser);
        Product existingProduct=productMapper.getProduct(productId);
        CartItems cartItems=cartItemsRepository.findByCartAndProduct(existingCart,existingProduct)
                .orElseThrow(()-> new CartItemsNotFoundException("Cart items not found"));
        cartItemsRepository.delete(cartItems);
        cartMapper.calculateTotalCartAmount(existingCart);
    }
}
