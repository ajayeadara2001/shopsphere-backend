package com.shopsphere.service;

import com.shopsphere.dto.RefundRequestDto;
import com.shopsphere.dto.RefundResponseDto;
import com.shopsphere.dto.RefundStatusRequestDto;

import java.util.List;

public interface RefundService {
    RefundResponseDto applyRefund(RefundRequestDto refundRequestDto);
    RefundResponseDto getRefundById(Long refundId);
    List<RefundResponseDto> getRefundByPaymentId(Long paymentId);
    List<RefundResponseDto> getAllRefunds();
    RefundResponseDto updateRefund(Long refundId, RefundStatusRequestDto refundStatusRequestDto);
}
