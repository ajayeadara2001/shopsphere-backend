package com.shopsphere.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shopsphere.constant.PaymentMethod;
import com.shopsphere.constant.PaymentStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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
    @JsonFormat(pattern = "yyyy-MM-ss HH:mm:ss")
    private LocalDateTime paidAt;
    private Long orderId;
}
