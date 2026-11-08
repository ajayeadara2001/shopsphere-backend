
package com.shopsphere.service.serviceImpl;

import com.shopsphere.constant.OrderStatus;
import com.shopsphere.constant.PaymentStatus;
import com.shopsphere.dto.PaymentRequestDto;
import com.shopsphere.dto.PaymentResponseDto;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.User;
import com.shopsphere.exception.AccessDeniedException;
import com.shopsphere.exception.OrderNotFoundException;
import com.shopsphere.exception.PaymentStatusException;
import com.shopsphere.mapper.PaymentMapper;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.OrderRepository;
import com.shopsphere.repository.PaymentRepository;
import com.shopsphere.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserMapper userMapper;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponseDto createPayment(Long orderId,PaymentRequestDto paymentRequestDto) {
        User existingUser = userMapper.getCurrentUser();
        // Lock the order first to prevent concurrent payment creation
        // and coordinate with order cancellation.
        Order order = orderRepository.findWithLockByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException
                        ("Order not found for Id: " + orderId));
        if (!order.getUser().getUserId().equals(existingUser.getUserId())) {
            throw new AccessDeniedException
                    ("You are not authorized to make payment for this order.");
        }
        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new PaymentStatusException
                    ("Payment can only be made for Placed order.");
        }
        Optional<Payment> latestPayment =
                paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order);
        if (latestPayment.isPresent()) {
            PaymentStatus paymentStatus =
                    latestPayment.get().getPaymentStatus();
            if (paymentStatus == PaymentStatus.PENDING) {
                throw new PaymentStatusException
                        ("A payment is already in PENDING for this order.");
            }
            if (paymentStatus == PaymentStatus.SUCCESS) {
                throw new PaymentStatusException
                        ("Payment is already Successful for this order.");
            }
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setUser(existingUser);
        payment.setAmount(order.getTotalOrderAmount());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(paymentRequestDto.getPaymentMethod());
        payment.setPaidAt(null);
        payment.setPaymentNumber("PAY_" + UUID.randomUUID());
        return paymentMapper.mapPaymentToResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentResponseDto successPayment(Long paymentId) {
        // Initial read to identify the associated order.
        Payment initialPayment = paymentMapper.getPayment(paymentId);
        Long orderId = initialPayment.getOrder().getOrderId();
        // Always acquire the order lock before the payment lock.
        Order order = orderRepository.findWithLockByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException
                        ("Order not found for Id: " + orderId));
        Payment payment = paymentMapper.getPaymentWithLock(paymentId);
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new PaymentStatusException
                    ("Payment is already in "+ payment.getPaymentStatus()+ " and cannot be updated.");
        }
        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new PaymentStatusException
                    ("Payment can only succeed for an order in PLACED status.");
        }
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        return paymentMapper.mapPaymentToResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentResponseDto failedPayment(Long paymentId) {
        // Initial read to identify the associated order.
        Payment initialPayment = paymentMapper.getPayment(paymentId);
        Long orderId = initialPayment.getOrder().getOrderId();
        // Keep the same lock order: Order -> Payment.
        Order order = orderRepository.findWithLockByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException
                        ("Order not found for Id: " + orderId));
        Payment payment = paymentMapper.getPaymentWithLock(paymentId);
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new PaymentStatusException
                    ("Payment is already in " + payment.getPaymentStatus() + " and cannot be updated.");
        }
        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new PaymentStatusException
                    ("Payment can only fail for an order in PLACED status.");
        }
        payment.setPaymentStatus(PaymentStatus.FAILED);
        // The order intentionally remains PLACED so the customer
        // can retry payment using a new payment attempt.
        return paymentMapper.mapPaymentToResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getAllPayments() {
        List<Payment> paymentList = paymentRepository.findAll();
        List<PaymentResponseDto> paymentResponseDtoList =new ArrayList<>();
        for (Payment payment : paymentList) {
            paymentResponseDtoList.add(paymentMapper.mapPaymentToResponse(payment));
        }
        return paymentResponseDtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getUserAllPayments() {
        User user = userMapper.getCurrentUser();
        List<Payment> paymentList = paymentRepository.findByUser(user);
        List<PaymentResponseDto> paymentResponseDtoList = new ArrayList<>();
        for (Payment payment : paymentList) {
            paymentResponseDtoList.add(paymentMapper.mapPaymentToResponse(payment));
        }
        return paymentResponseDtoList;
    }
}
