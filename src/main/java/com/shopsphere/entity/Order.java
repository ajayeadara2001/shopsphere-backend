package com.shopsphere.entity;

import com.shopsphere.constant.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;
    @Column(nullable = false, unique = true, updatable = false)
    private String orderNumber;
    private BigDecimal totalOrderAmount;
    @CreationTimestamp
    private LocalDateTime orderedAt;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
    @OneToMany(mappedBy = "order",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<OrderItems> orderItemsList=new ArrayList<>();
    private String couponCode;
    private BigDecimal discountAmount;

}
