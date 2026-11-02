package com.shopsphere.mapper;

import com.shopsphere.dto.RefundResponseDto;
import com.shopsphere.entity.Refund;
import com.shopsphere.exception.RefundNotFoundException;
import com.shopsphere.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundMapper {
    private final RefundRepository refundRepository;
    public RefundResponseDto mapRefundToResponse(Refund refund) {
        return new RefundResponseDto(
                refund.getRefundId(),
                refund.getPayment().getPaymentId(),
                refund.getPayment().getPaymentNumber(),
                refund.getRefundAmount(),
                refund.getRefundReason(),
                refund.getRefundStatus(),
                refund.getCreatedAt(),
                refund.getUpdatedAt());
    }
    public Refund getRefundById(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new RefundNotFoundException
                        ("Refund not found for Id: " + refundId));
    }
}
