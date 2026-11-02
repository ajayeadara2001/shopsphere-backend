package com.shopsphere.repository;

import com.shopsphere.entity.Order;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findWithLockByPaymentId(Long paymentId);
    Optional<Payment> findFirstByOrderOrderByPaymentIdDesc(Order order);
    List<Payment> findByUser(User user);
}
