package com.shopsphere.controller;


import com.shopsphere.dto.OnCreate;
import com.shopsphere.dto.RefundRequestDto;
import com.shopsphere.dto.RefundResponseDto;
import com.shopsphere.service.RefundService;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/refund")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<RefundResponseDto> applyRefund(
            @Validated({Default.class, OnCreate.class})
            @RequestBody RefundRequestDto refundRequestDto){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(refundService.applyRefund(refundRequestDto));
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponseDto> getRefundId(
            @PathVariable Long refundId){
        return ResponseEntity.ok(refundService.getRefundById(refundId));
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<RefundResponseDto>> getRefundByPaymentId(
            @PathVariable Long paymentId){
        return ResponseEntity.ok(refundService.getRefundByPaymentId(paymentId));
    }
}
