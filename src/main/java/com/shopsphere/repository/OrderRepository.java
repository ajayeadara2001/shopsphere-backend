package com.shopsphere.repository;

import com.shopsphere.constant.OrderStatus;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.Product;
import com.shopsphere.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order> findByUser(User user);
    boolean existsByUserAndOrderStatusAndOrderItemsListProduct
            (User user, OrderStatus orderStatus, Product product);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findWithLockByOrderId(@Param("orderId") Long orderId);
}
