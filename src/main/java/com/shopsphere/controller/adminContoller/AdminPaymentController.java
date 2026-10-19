package com.shopsphere.controller.adminContoller;

import com.shopsphere.dto.PaymentResponseDto;
import com.shopsphere.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {
    private final PaymentService paymentService;

    @PutMapping("/{paymentId}/success")
    public ResponseEntity<PaymentResponseDto> successPayment(@PathVariable Long paymentId){
        return ResponseEntity.ok(paymentService.successPayment(paymentId));
    }

    @PutMapping("/{paymentId}/failed")
    public ResponseEntity<PaymentResponseDto> failedPayment(@PathVariable Long paymentId){
        return ResponseEntity.ok(paymentService.failedPayment(paymentId));
    }

    @GetMapping("/payment-history")
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments(){
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
}
