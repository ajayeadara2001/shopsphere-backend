package com.shopsphere.service.serviceImpl;

import com.shopsphere.constant.PaymentStatus;
import com.shopsphere.constant.RefundStatus;
import com.shopsphere.dto.RefundRequestDto;
import com.shopsphere.dto.RefundResponseDto;
import com.shopsphere.dto.RefundStatusRequestDto;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.Refund;
import com.shopsphere.entity.User;
import com.shopsphere.exception.*;
import com.shopsphere.mapper.PaymentMapper;
import com.shopsphere.mapper.RefundMapper;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.PaymentRepository;
import com.shopsphere.repository.RefundRepository;
import com.shopsphere.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final RefundMapper refundMapper;
    private final UserMapper userMapper;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public RefundResponseDto applyRefund(RefundRequestDto refundRequestDto) {
        User currentUser=userMapper.getCurrentUser();
        Payment payment=paymentMapper.getPayment(refundRequestDto.getPaymentId());
        if(payment.getUser().equals(currentUser)){
            throw new AccessDeniedException
            ("You are not authorized to request a refund for this payment.");

        }
        if(payment.getPaymentStatus()!= PaymentStatus.SUCCESS){
            throw new PaymentStatusException
                    ("Refund can only be requested for successful payments.");
        }
        List<Refund> refundList=refundRepository.findByPaymentPaymentId(payment.getPaymentId());
        BigDecimal alreadyRefunded=refundList.stream()
                .filter(refund -> refund.getRefundStatus()== RefundStatus.COMPLETED)
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal refundableAmount=payment.getAmount().subtract(alreadyRefunded);
        if(refundRequestDto.getRefundAmount().compareTo(refundableAmount)>0){
            throw new RefundException("Refund amount exceeds refundable amount.");
        }
        Refund refund=new Refund();
        refund.setPayment(payment);
        refund.setRefundAmount(refundRequestDto.getRefundAmount());
        refund.setRefundReason(refundRequestDto.getRefundReason());
        refund.setRefundStatus(RefundStatus.PENDING);
        return refundMapper.mapRefundToResponse(refundRepository.save(refund));
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundById(Long refundId) {
        return refundMapper.mapRefundToResponse
                (refundMapper.getRefundById(refundId));
    }

    @Override
    @Transactional
    public List<RefundResponseDto> getRefundByPaymentId(Long paymentId) {
        if(!paymentRepository.existsById(paymentId)){
            throw new PaymentNotFoundException("Payment not found for Id: "+paymentId);
        }
        List<Refund> refundList=refundRepository.findByPaymentPaymentId(paymentId);
        List<RefundResponseDto> refundResponseDtoList=new ArrayList<>();
        for(Refund refund:refundList){
            refundResponseDtoList.add(refundMapper.mapRefundToResponse(refund));
        }
        return refundResponseDtoList;
    }

    @Override
    public List<RefundResponseDto> getAllRefunds() {
        List<Refund> refundList=refundRepository.findAll();
        List<RefundResponseDto> refundResponseDtoList=new ArrayList<>();
        for(Refund refund:refundList){
            refundResponseDtoList.add(refundMapper.mapRefundToResponse(refund));
        }
        return refundResponseDtoList;
    }

    @Override
    public RefundResponseDto updateRefund(Long refundId, RefundStatusRequestDto refundStatusRequestDto) {
        Refund refund=refundMapper.getRefundById(refundId);
        RefundStatus currentStatus=refund.getRefundStatus();
        RefundStatus newStatus=refundStatusRequestDto.getRefundStatus();
        if(currentStatus!=RefundStatus.PENDING){
            throw new RefundException("Only pending refunds can be updated.");
        }
        if(newStatus==RefundStatus.COMPLETED){
            Payment payment=refund.getPayment();
            if(payment.getPaymentStatus()!=PaymentStatus.SUCCESS){
                throw new PaymentStatusException("Payment must be successful before refund");
            }
            refund.setRefundStatus(RefundStatus.COMPLETED);
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
            refundRepository.save(refund);
        }else if(newStatus==RefundStatus.FAILED){
            refund.setRefundStatus(RefundStatus.FAILED);
            refundRepository.save(refund);
        }else{
            throw new RefundException("Invalid Refund status transaction");
        }
        return refundMapper.mapRefundToResponse(refundRepository.save(refund));
    }
}
