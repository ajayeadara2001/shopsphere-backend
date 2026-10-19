package com.shopsphere.controller;

import com.shopsphere.dto.PaymentRequestDto;
import com.shopsphere.dto.PaymentResponseDto;
import com.shopsphere.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/{orderId}")
    public ResponseEntity<PaymentResponseDto> createPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequestDto paymentRequestDto){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.createPayment(orderId,paymentRequestDto));
    }

    @GetMapping("/me")
    public ResponseEntity<List<PaymentResponseDto>> getUserAllPayments(){
        return ResponseEntity.ok(paymentService.getUserAllPayments());
    }
}
