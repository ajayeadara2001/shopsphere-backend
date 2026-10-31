package com.shopsphere.controller.adminContoller;


import com.shopsphere.dto.OnCreate;
import com.shopsphere.dto.RefundResponseDto;
import com.shopsphere.dto.RefundStatusRequestDto;
import com.shopsphere.service.RefundService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/refund")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRefundController {
    private final RefundService refundService;

    @GetMapping("/all")
    public ResponseEntity<List<RefundResponseDto>> getAllRefunds(){
        return ResponseEntity.ok(refundService.getAllRefunds());
    }

    @PutMapping("/{refundId}/status")
    public ResponseEntity<RefundResponseDto> updateRefund(
            @Valid @RequestBody RefundStatusRequestDto refundStatusRequestDto,
            @PathVariable Long refundId){
        return ResponseEntity
                .ok(refundService.updateRefund(refundId,refundStatusRequestDto));
    }
}
