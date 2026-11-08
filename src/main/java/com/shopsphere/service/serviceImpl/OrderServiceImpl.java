
package com.shopsphere.service.serviceImpl;

import com.shopsphere.constant.OrderStatus;
import com.shopsphere.constant.PaymentStatus;
import com.shopsphere.constant.RefundStatus;
import com.shopsphere.constant.Role;
import com.shopsphere.dto.OrderResponseDto;
import com.shopsphere.dto.OrderStatusUpdateRequestDto;
import com.shopsphere.entity.*;
import com.shopsphere.exception.*;
import com.shopsphere.mapper.CartMapper;
import com.shopsphere.mapper.OrderMapper;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.*;
import com.shopsphere.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final UserMapper userMapper;
    private final CartMapper cartMapper;
    private final OrderMapper orderMapper;

    private void requireNext(OrderStatus proposed, OrderStatus... allowed) {
        for (OrderStatus status : allowed) {
            if (status == proposed) {
                return;
            }
        }
        throw new OrderStatusException("Invalid order status transition.");
    }

    private Order getOrderWithLock(Long orderId) {
        return orderRepository.findWithLockByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException
                        ("Order not found for Id: " + orderId));
    }

    private void restoreStock(Order order) {
        List<OrderItems> sortedItems = new ArrayList<>
                (order.getOrderItemsList());
        sortedItems.sort(Comparator.comparing
                (item -> item.getProduct().getProductId()));

        for (OrderItems item : sortedItems) {
            Long productId = item.getProduct().getProductId();
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() ->new ProductNotFoundException("Product not found"));
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }
    }

    private void createCancellationRefund(Order order) {
        Payment latestPayment = paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)
                .orElseThrow(() -> new PaymentStatusException
                        ("Confirmed order has no payment record."));
        Payment payment = paymentRepository
                .findWithLockByPaymentId(latestPayment.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException
                        ("Payment not found for Id: "+ latestPayment.getPaymentId()));
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS
                && payment.getPaymentStatus() != PaymentStatus.REFUNDED) {
            throw new PaymentStatusException(
                    "Confirmed order does not have a reconciled payment. "
                            + "Cancellation requires payment reconciliation.");
        }
        BigDecimal alreadyCommitted = refundRepository
                .findByPaymentPaymentId(payment.getPaymentId())
                .stream()
                .filter(refund ->
                        refund.getRefundStatus() == RefundStatus.PENDING
                                || refund.getRefundStatus() == RefundStatus.COMPLETED)
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingRefundable = payment.getAmount()
                .subtract(alreadyCommitted);
        if (remainingRefundable.signum() < 0) {
            throw new RefundException
                    ("Existing refunds exceed the original payment amount.");
        }
        if (remainingRefundable.signum() > 0) {
            Refund refund = new Refund();
            refund.setPayment(payment);
            refund.setRefundAmount(remainingRefundable);
            refund.setRefundReason
                    ("Refund due to admin cancellation of order "+ order.getOrderNumber());
            refund.setRefundStatus(RefundStatus.PENDING);
            refundRepository.save(refund);
        }
    }

    private void validateOrderStatus(OrderStatus currentStatus, OrderStatus newStatus) {
        switch (currentStatus) {
            case PLACED -> requireNext(newStatus,OrderStatus.CONFIRMED,OrderStatus.CANCELLED);
            case CONFIRMED -> requireNext(newStatus,OrderStatus.SHIPPED,OrderStatus.CANCELLED);
            case SHIPPED -> requireNext(newStatus,OrderStatus.OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY -> requireNext(newStatus,OrderStatus.DELIVERED);
            case DELIVERED, CANCELLED -> throw new OrderStatusException
                    ("Order is already " + currentStatus + ".");
        }
    }

    private void validateCoupon(Coupon coupon, BigDecimal cartTotal) {
        if (!Boolean.TRUE.equals(coupon.getActiveStatus())) {
            throw new CouponException("Coupon is inactive");
        }
        if (coupon.getExpiryDate() == null
                || !coupon.getExpiryDate().isAfter(LocalDateTime.now())) {
            throw new CouponException("Coupon is expired");
        }
        if (coupon.getUsageLimit() != null
                && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw new CouponException("Coupon usage limit exceeded");
        }
        if (cartTotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new CouponException
                    ("Minimum order amount is "+ coupon.getMinimumOrderAmount());
        }
    }

    @Override
    @Transactional
    public OrderResponseDto placeOrder(String couponCode) {
        User user = userMapper.getCurrentUser();
        Cart cart = cartMapper.getCartForUser(user);
        if (cart.getCartItemsList().isEmpty()
                || cart.getTotalAmount() == null
                || cart.getTotalAmount().signum() <= 0) {
            throw new IllegalArgumentException
                    ("Cannot place an order from an empty cart.");
        }
        BigDecimal cartTotal = cart.getTotalAmount();
        BigDecimal discount = BigDecimal.ZERO;
        Coupon coupon = null;
        if (couponCode != null && !couponCode.isBlank()) {
            coupon = couponRepository.findByCouponCode
                    (couponCode.trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() ->new CouponNotFoundException("Invalid coupon code"));
            validateCoupon(coupon, cartTotal);
            discount = cartTotal.multiply(coupon.getDiscountPercentage())
                    .divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
            coupon.setUsageCount(coupon.getUsageCount() + 1);
            couponRepository.save(coupon);
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setCouponCode(coupon == null ? null : coupon.getCouponCode());
        order.setDiscountAmount(discount);
        order.setTotalOrderAmount(cartTotal.subtract(discount));
        List<CartItems> sortedCartItems = new ArrayList<>(cart.getCartItemsList());
        sortedCartItems.sort(Comparator.comparing
                (item -> item.getProduct().getProductId()));

        for (CartItems cartItem : sortedCartItems) {
            Long productId = cartItem.getProduct().getProductId();
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductNotFoundException
                            ("Product not found"));
            if (!Boolean.TRUE.equals(product.getActive())
                    || product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalArgumentException
                        ("Product is unavailable in the requested quantity.");
            }
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
            OrderItems item = new OrderItems();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(cartItem.getQuantity());
            item.setPriceAtAdded(cartItem.getPriceAtAdded());
            order.getOrderItemsList().add(item);
        }
        orderRepository.save(order);
        cart.getCartItemsList().clear();
        cartMapper.calculateTotalCartAmount(cart);
        cartRepository.save(cart);
        return orderMapper.mapOrderToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getUserOrders() {
        return orderRepository.findByUser(userMapper.getCurrentUser())
                .stream()
                .map(orderMapper::mapOrderToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long orderId) {
        User user = userMapper.getCurrentUser();
        Order order = orderMapper.getOrder(orderId);
        if (user.getRole() != Role.ADMIN
                && !order.getUser().getUserId().equals(user.getUserId())) {
            throw new OrderNotFoundException
                    ("Order not found for Id: " + orderId);
        }
        return orderMapper.mapOrderToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::mapOrderToResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId,OrderStatusUpdateRequestDto request) {
        Order order = getOrderWithLock(orderId);
        validateOrderStatus(order.getOrderStatus(),request.getOrderStatus());
        if (request.getOrderStatus() == OrderStatus.CANCELLED) {
            if (order.getOrderStatus() == OrderStatus.CONFIRMED) {
                createCancellationRefund(order);
            }
            restoreStock(order);
        }
        order.setOrderStatus(request.getOrderStatus());
        return orderMapper.mapOrderToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(Long orderId) {
        User user = userMapper.getCurrentUser();
        Order order = getOrderWithLock(orderId);
        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException
                    ("You are not authorized to cancel this order.");
        }
        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new OrderStatusException
                    ("Order cannot be cancelled from "+ order.getOrderStatus() + ".");
        }
        restoreStock(order);
        order.setOrderStatus(OrderStatus.CANCELLED);
        return orderMapper.mapOrderToResponse(
                orderRepository.save(order)
        );
    }
}
