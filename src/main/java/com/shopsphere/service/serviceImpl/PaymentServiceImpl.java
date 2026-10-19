package com.shopsphere.service.serviceImpl;

import com.shopsphere.constant.OrderStatus;
import com.shopsphere.constant.PaymentStatus;
import com.shopsphere.dto.PaymentRequestDto;
import com.shopsphere.dto.PaymentResponseDto;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.User;
import com.shopsphere.exception.AccessDeniedException;
import com.shopsphere.exception.PaymentStatusException;
import com.shopsphere.mapper.OrderMapper;
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

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponseDto createPayment(Long orderId, PaymentRequestDto paymentRequestDto) {
        User existingUser=userMapper.getCurrentUser();
        Order order=orderMapper.getOrder(orderId);
        if(order.getOrderStatus()!= OrderStatus.PLACED){
            throw new PaymentStatusException("Payment can only be maid for Placed order.");
        }
        if(!order.getUser().getUserId().equals(existingUser.getUserId())){
            throw new AccessDeniedException("You are not authorized to make payment for this order.");
        }
        Optional<Payment> latestPayment=
                paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order);
        if(latestPayment.isPresent()){
            PaymentStatus paymentStatus=latestPayment.get().getPaymentStatus();
            if(paymentStatus==PaymentStatus.PENDING){
                throw new PaymentStatusException("A payment is already in PENDING for this order.");
            }
            if(paymentStatus==PaymentStatus.SUCCESS){
                throw new PaymentStatusException("Payment is already Successful for this order");
            }
        }
        Payment payment=new Payment();
        payment.setOrder(order);
        payment.setUser(existingUser);
        payment.setAmount(order.getTotalOrderAmount());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(paymentRequestDto.getPaymentMethod());
        payment.setPaidAt(null);
        payment.setPaymentNumber("PAY_"+System.currentTimeMillis());
        return paymentMapper.mapPaymentToResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentResponseDto successPayment(Long paymentId) {
        Payment payment=paymentMapper.getPayment(paymentId);
        if(payment.getPaymentStatus()!=PaymentStatus.PENDING){
            throw new PaymentStatusException(
                    "Payment is already in "+payment.getPaymentStatus()+ " and cannot be updated");
        }
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        Order order=payment.getOrder();
        order.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        return paymentMapper.mapPaymentToResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentResponseDto failedPayment(Long paymentId) {
        Payment payment=paymentMapper.getPayment(paymentId);
        if(payment.getPaymentStatus()!=PaymentStatus.PENDING){
            throw new PaymentStatusException(
                    "Payment is already in "+payment.getPaymentStatus()+ " and cannot be updated");
        }
        payment.setPaymentStatus(PaymentStatus.FAILED);
        return paymentMapper.mapPaymentToResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public List<PaymentResponseDto> getAllPayments() {
        List<Payment> paymentList = paymentRepository.findAll();
        List<PaymentResponseDto> paymentResponseDtoList = new ArrayList<>();
        for (Payment payment : paymentList) {
            paymentResponseDtoList.add(paymentMapper.mapPaymentToResponse(payment));
        }
        return paymentResponseDtoList;

    }
    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getUserAllPayments() {
        User user=userMapper.getCurrentUser();
        List<Payment> paymentList=paymentRepository.findByUser(user);
        List<PaymentResponseDto>paymentResponseDtoList=new ArrayList<>();
        for(Payment payment:paymentList){
            paymentResponseDtoList.add(paymentMapper.mapPaymentToResponse(payment));
        }
        return paymentResponseDtoList;
    }
}
