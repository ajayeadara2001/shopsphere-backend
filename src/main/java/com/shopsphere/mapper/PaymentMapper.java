package com.shopsphere.mapper;

import com.shopsphere.dto.PaymentResponseDto;
import com.shopsphere.entity.Payment;
import com.shopsphere.exception.PaymentNotFoundException;
import com.shopsphere.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMapper {
    private final PaymentRepository paymentRepository;
    public PaymentResponseDto mapPaymentToResponse(Payment payment){
        return new PaymentResponseDto(
                payment.getPaymentId(),
                payment.getPaymentNumber(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                payment.getPaymentMethod(),
                payment.getPaidAt(),
                payment.getOrder().getOrderId()
        );
    }
    public Payment getPayment(Long paymentId){
        return paymentRepository.findById(paymentId)
                .orElseThrow(()->new PaymentNotFoundException("Payment not found for Id: "+paymentId));
    }
}
