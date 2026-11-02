package com.shopsphere.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shopsphere.constant.RefundStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundResponseDto {
    private Long refundId;
    private Long paymentId;
    private String paymentNumber;
    private BigDecimal refundAmount;
    private String refundReason;
    private RefundStatus refundStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
