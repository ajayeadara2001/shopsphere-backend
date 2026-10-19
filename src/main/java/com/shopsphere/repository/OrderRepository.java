package com.shopsphere.repository;

import com.shopsphere.entity.Order;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order> findByUser(User user);
    boolean existsByUserAndOrderItemsListProduct(User user, Product product);
}
