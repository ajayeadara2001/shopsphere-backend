package com.shopsphere.repository;

import com.shopsphere.entity.Cart;
import com.shopsphere.entity.CartItems;
import com.shopsphere.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemsRepository extends JpaRepository<CartItems, Long> {
    Optional<CartItems> findByCartAndProduct(Cart cart, Product product);
    List<CartItems> findByCart(Cart cart);
}
