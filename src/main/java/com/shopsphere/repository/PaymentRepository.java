package com.shopsphere.repository;

import com.shopsphere.entity.Order;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    Optional<Payment> findFirstByOrderOrderByPaymentIdDesc(Order order);
    List<Payment> findByUser(User user);
}
