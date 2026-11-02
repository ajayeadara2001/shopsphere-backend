package com.shopsphere.service.serviceImpl;

import com.shopsphere.constant.PaymentStatus;
import com.shopsphere.constant.RefundStatus;
import com.shopsphere.constant.Role;
import com.shopsphere.dto.*;
import com.shopsphere.entity.*;
import com.shopsphere.exception.*;
import com.shopsphere.mapper.*;
import com.shopsphere.repository.*;
import com.shopsphere.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final RefundMapper refundMapper;
    private final UserMapper userMapper;
    private final PaymentMapper paymentMapper;

    private void authorize(Payment payment) {
        User user = userMapper.getCurrentUser();
        if (user.getRole() != Role.ADMIN
                && !payment.getUser().getUserId().equals(user.getUserId())){
            throw new AccessDeniedException
                    ("You are not authorized to access this refund.");
        }
    }

    @Override
    @Transactional
    public RefundResponseDto applyRefund(RefundRequestDto request) {
        User user = userMapper.getCurrentUser();
        Payment payment = paymentMapper.getPaymentWithLock(request.getPaymentId());
        if (!payment.getUser().getUserId().equals(user.getUserId())){
            throw new AccessDeniedException
                    ("You are not authorized to request a refund for this payment.");
        }
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS){
            throw new PaymentStatusException
                    ("Refund can only be requested for successful payments.");
        }
        BigDecimal committed = refundRepository.findByPaymentPaymentId
                        (payment.getPaymentId()).stream().filter(r ->
                        r.getRefundStatus() == RefundStatus.PENDING ||
                        r.getRefundStatus() == RefundStatus.COMPLETED)
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (request.getRefundAmount().compareTo(payment.getAmount().subtract(committed)) > 0){
            throw new RefundException("Refund amount exceeds refundable amount.");
        }
        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setRefundAmount(request.getRefundAmount());
        refund.setRefundReason(request.getRefundReason());
        refund.setRefundStatus(RefundStatus.PENDING);
        return refundMapper.mapRefundToResponse(refundRepository.save(refund));
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundById(Long refundId) {
        Refund refund = refundMapper.getRefundById(refundId);
        authorize(refund.getPayment());
        return refundMapper.mapRefundToResponse(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponseDto> getRefundByPaymentId(Long paymentId) {
        Payment payment = paymentMapper.getPayment(paymentId);
        authorize(payment);
        return refundRepository.findByPaymentPaymentId(paymentId)
                .stream()
                .map(refundMapper::mapRefundToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponseDto> getAllRefunds() {
        return refundRepository.findAll()
                .stream()
                .map(refundMapper::mapRefundToResponse)
                .toList(); }

    @Override
    @Transactional
    public RefundResponseDto updateRefund(Long refundId,RefundStatusRequestDto request) {
        Refund refund = refundMapper.getRefundById(refundId);
        Payment payment = paymentMapper.getPaymentWithLock(
                refund.getPayment().getPaymentId()
        );
        if (refund.getRefundStatus() != RefundStatus.PENDING) {
            throw new RefundException("Only pending refunds can be updated.");
        }
        if (request.getRefundStatus() == RefundStatus.COMPLETED) {
            if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
                throw new PaymentStatusException(
                        "Payment must be successful before refund."
                );
            }
            refund.setRefundStatus(RefundStatus.COMPLETED);
            BigDecimal completed = refundRepository
                    .findByPaymentPaymentId(payment.getPaymentId())
                    .stream()
                    .filter(item ->
                            item.getRefundStatus() == RefundStatus.COMPLETED)
                    .map(Refund::getRefundAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (completed.compareTo(payment.getAmount()) >= 0) {
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }

        } else if (request.getRefundStatus() == RefundStatus.FAILED) {
            refund.setRefundStatus(RefundStatus.FAILED);

        } else {
            throw new RefundException("Refund status must be COMPLETED or FAILED.");
        }
        return refundMapper.mapRefundToResponse(
                refundRepository.save(refund)
        );
    }
}
