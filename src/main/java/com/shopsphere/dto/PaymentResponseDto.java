package com.shopsphere.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shopsphere.constant.PaymentMethod;
import com.shopsphere.constant.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponseDto {
    private Long paymentId;
    private String paymentNumber;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;
    private Long orderId;
}
