package com.shopsphere.service;

import com.shopsphere.dto.PaymentRequestDto;
import com.shopsphere.dto.PaymentResponseDto;

import java.util.List;

public interface PaymentService {
    PaymentResponseDto createPayment(Long orderId, PaymentRequestDto paymentRequestDto);
    PaymentResponseDto successPayment(Long paymentId);
    PaymentResponseDto failedPayment(Long paymentId);
    List<PaymentResponseDto> getAllPayments();
    List<PaymentResponseDto> getUserAllPayments();

}
