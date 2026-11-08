package com.shopsphere.service.serviceImplTest;

import com.shopsphere.constant.OrderStatus;
import com.shopsphere.constant.PaymentStatus;
import com.shopsphere.constant.RefundStatus;
import com.shopsphere.constant.Role;
import com.shopsphere.dto.OrderResponseDto;
import com.shopsphere.dto.OrderStatusUpdateRequestDto;
import com.shopsphere.entity.*;
import com.shopsphere.exception.*;
import com.shopsphere.mapper.*;
import com.shopsphere.repository.*;
import static org.junit.jupiter.api.Assertions.*;

import com.shopsphere.service.serviceImpl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CartMapper cartMapper;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Test
    void updateOrderStatus_AdminCancelsPaidConfirmedOrder_ShouldRefundAndRestoreStock() {
        Order order = Order.builder()
                .orderId(1L)
                .orderNumber("ORD-TEST-001")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        Product product = new Product();
        product.setProductId(101L);
        product.setStockQuantity(8);

        OrderItems orderItems = OrderItems.builder()
                .quantity(2)
                .order(order)
                .product(product)
                .build();

        order.setOrderItemsList(List.of(orderItems));

        Payment payment = Payment.builder()
                .paymentId(2L)
                .paymentNumber("PAY-TEST-001")
                .amount(new BigDecimal("1000.00"))
                .order(order)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();
        Refund completedRefund = Refund.builder()
                .refundId(3L)
                .refundAmount(new BigDecimal("100.00"))
                .refundStatus(RefundStatus.COMPLETED)
                .payment(payment)
                .build();

        Refund pendingRefund = Refund.builder()
                .refundId(4L)
                .refundAmount(new BigDecimal("150.00"))
                .payment(payment)
                .refundStatus(RefundStatus.PENDING)
                .build();

        when(orderRepository.findWithLockByOrderId(1L))
                .thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findWithLockByPaymentId(2L))
                .thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentPaymentId(2L))
                .thenReturn(List.of(completedRefund, pendingRefund));
        when(productRepository.findByIdWithLock(101L))
                .thenReturn(Optional.of(product));

        OrderStatusUpdateRequestDto orderStatusUpdateRequestDto =
                OrderStatusUpdateRequestDto.builder()
                        .orderStatus(OrderStatus.CANCELLED)
                        .build();
        OrderResponseDto expectedOrderResponseDto =
                OrderResponseDto.builder()
                        .orderId(1L)
                        .orderNumber("ORD-TEST-001")
                        .orderStatus(OrderStatus.CANCELLED)
                        .build();

        when(orderMapper.mapOrderToResponse(order))
                .thenReturn(expectedOrderResponseDto);
        when(orderRepository.save(order))
                .thenReturn(order);

        OrderResponseDto actualOrderResponseDto =
                orderServiceImpl.updateOrderStatus(1L, orderStatusUpdateRequestDto);

        assertEquals(OrderStatus.CANCELLED, actualOrderResponseDto.getOrderStatus());

        ArgumentCaptor<Refund> refundArgumentCaptor =
                ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository, times(1))
                .save(refundArgumentCaptor.capture());
        Refund actualRefund = refundArgumentCaptor.getValue();

        assertEquals(0, new BigDecimal("750.00")
                .compareTo(actualRefund.getRefundAmount()));
        assertEquals(RefundStatus.PENDING, actualRefund.getRefundStatus());
        assertEquals(10, product.getStockQuantity());
        verify(productRepository, times(1)).save(product);
        verify(paymentRepository, times(1)).findWithLockByPaymentId(2L);
        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void cancelOrder_CustomerCancelsUnpaidOrder_ShouldRestoreStockWithoutRefund() {
        Order order = Order.builder()
                .orderId(2L)
                .orderNumber("ORD-TEST-002")
                .orderStatus(OrderStatus.PLACED)
                .build();
        Product product = new Product();
        product.setStockQuantity(8);
        product.setProductId(102L);

        OrderItems orderItems = OrderItems.builder()
                .product(product)
                .order(order)
                .quantity(2)
                .build();

        order.setOrderItemsList(List.of(orderItems));

        when(orderRepository.findWithLockByOrderId(2L)).thenReturn(Optional.of(order));
        User customer = User.builder()
                .userId(10L)
                .build();
        order.setUser(customer);
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(productRepository.findByIdWithLock(102L)).thenReturn(Optional.of(product));

        OrderResponseDto expectedResponse = OrderResponseDto.builder()
                .orderId(2L)
                .orderStatus(OrderStatus.CANCELLED)
                .build();
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order)).thenReturn(expectedResponse);
        orderServiceImpl.cancelOrder(2L);
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(10, product.getStockQuantity());
        verify(productRepository, times(1)).save(product);
        verifyNoInteractions(paymentRepository, refundRepository);
    }

    @Test
    void updateOrderStatus_AdminCancelsShippedOrder_ShouldThrowInvalidTransition() {
        Order order = Order.builder()
                .orderId(3L)
                .orderNumber("ORD-TEST-003")
                .orderStatus(OrderStatus.SHIPPED)
                .build();

        when(orderRepository.findWithLockByOrderId(3L))
                .thenReturn(Optional.of(order));
        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CANCELLED)
                .build();
        assertThrows(OrderStatusException.class,
                () -> orderServiceImpl.updateOrderStatus(3L, request));
        assertEquals(OrderStatus.SHIPPED, order.getOrderStatus());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(productRepository, paymentRepository, refundRepository);
    }

    @Test
    void cancelOrder_CustomerCancelsAnotherCustomersOrder_ShouldThrowAccessDeniedException() {
        Order order = Order.builder()
                .orderId(4L)
                .orderNumber("ORD-TEST-004")
                .orderStatus(OrderStatus.PLACED)
                .build();

        User orderOwner = new User();
        orderOwner.setUserId(20L);
        order.setUser(orderOwner);

        User currentUser = new User();
        currentUser.setUserId(10L);

        when(userMapper.getCurrentUser()).thenReturn(currentUser);
        when(orderRepository.findWithLockByOrderId(4L))
                .thenReturn(Optional.of(order));
        assertThrows(
                AccessDeniedException.class,
                () -> orderServiceImpl.cancelOrder(4L));
        assertEquals(OrderStatus.PLACED, order.getOrderStatus());

        verify(userMapper, times(1)).getCurrentUser();
        verify(orderRepository, times(1)).findWithLockByOrderId(4L);
        verify(orderRepository, never()).save(any());

        verifyNoInteractions(productRepository, paymentRepository, refundRepository);
    }

    @Test
    void cancelOrder_CustomerCancelsConfirmedOrder_ShouldThrowOrderStatusException() {
        Order order = Order.builder()
                .orderId(5L)
                .orderNumber("ORD-TEST-005")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        User orderOwner = new User();
        orderOwner.setUserId(10L);
        order.setUser(orderOwner);

        User currentUser = new User();
        currentUser.setUserId(10L);

        when(userMapper.getCurrentUser()).thenReturn(currentUser);
        when(orderRepository.findWithLockByOrderId(5L))
                .thenReturn(Optional.of(order));
        assertThrows(
                OrderStatusException.class,
                () -> orderServiceImpl.cancelOrder(5L));

        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(productRepository, paymentRepository, refundRepository);
    }

    @Test
    void cancelOrder_CustomerCancelsNonExistingOrder_ShouldThrowOrderNotFoundException() {
        User currentUser = new User();
        currentUser.setUserId(10L);
        when(userMapper.getCurrentUser()).thenReturn(currentUser);
        when(orderRepository.findWithLockByOrderId(6L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderServiceImpl.cancelOrder(6L));
        verify(userMapper).getCurrentUser();
        verify(orderRepository).findWithLockByOrderId(6L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).save(any(Product.class));
        verify(refundRepository, never()).save(any(Refund.class));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void cancelOrder_CustomerCancelsAlreadyCancelledOrder_ShouldThrowOrderStatusException() {
        Order order = Order.builder()
                .orderId(7L)
                .orderNumber("ORD-TEST-007")
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        User ownedUser = new User();
        ownedUser.setUserId(11L);
        order.setUser(ownedUser);
        when(orderRepository.findWithLockByOrderId(7L)).thenReturn(Optional.of(order));
        when(userMapper.getCurrentUser()).thenReturn(ownedUser);
        assertThrows(OrderStatusException.class,
                () -> orderServiceImpl.cancelOrder(7L));
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository, refundRepository, paymentRepository);
    }

    @Test
    void cancelOrder_CustomerCancelsPaidConfirmedOrder_ShouldThrowAndNotRefundOrRestoreStock() {
        User customer = User.builder()
                .userId(12L)
                .build();

        Order order = Order.builder()
                .orderId(8L)
                .orderNumber("ORD-TEST-008")
                .orderStatus(OrderStatus.CONFIRMED)
                .user(customer)
                .build();

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(orderRepository.findWithLockByOrderId(8L)).thenReturn(Optional.of(order));
        assertThrows(
                OrderStatusException.class,
                () -> orderServiceImpl.cancelOrder(8L));

        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verifyNoInteractions(productRepository, paymentRepository, refundRepository);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_AdminCancelsPaidOrderWithExistingRefund_ShouldRefundRemainingAmount() {
        Order order = Order.builder()
                .orderId(9L)
                .orderNumber("ORD-TEST-009")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        Product product = new Product();
        product.setProductId(103L);
        product.setStockQuantity(8);

        OrderItems orderItems = OrderItems.builder()
                .product(product)
                .order(order)
                .quantity(2)
                .build();
        order.setOrderItemsList(List.of(orderItems));

        Payment payment = Payment.builder()
                .paymentId(9L)
                .paymentNumber("PAY-TEST-009")
                .order(order)
                .amount(new BigDecimal("1000.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        Refund existingRefund = Refund.builder()
                .refundId(9L)
                .payment(payment)
                .refundAmount(new BigDecimal("300.00"))
                .refundStatus(RefundStatus.COMPLETED)
                .build();

        OrderResponseDto expectedResponse = OrderResponseDto.builder()
                .orderId(9L)
                .orderNumber("ORD-TEST-009")
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findWithLockByOrderId(9L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.of(payment));
        when(paymentRepository.findWithLockByPaymentId(9L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentPaymentId(9L)).thenReturn(List.of(existingRefund));
        when(productRepository.findByIdWithLock(103L)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order)).thenReturn(expectedResponse);

        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        orderServiceImpl.updateOrderStatus(9L, request);

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository, times(1)).save(refundCaptor.capture());
        Refund newRefund = refundCaptor.getValue();

        assertEquals(0, new BigDecimal("700.00").compareTo(newRefund.getRefundAmount()));
        assertEquals(RefundStatus.PENDING, newRefund.getRefundStatus());
        assertSame(payment, newRefund.getPayment());
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(10, product.getStockQuantity());
        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());

        verify(productRepository, times(1)).save(product);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void updateOrderStatus_AdminCancelsPlacedOrder_ShouldRestoreStockWithoutRefund() {
        Order order = Order.builder()
                .orderId(10L).orderNumber("ORD-TEST-010")
                .orderStatus(OrderStatus.PLACED).build();
        Product product = new Product();
        product.setProductId(104L);
        product.setStockQuantity(5);
        order.setOrderItemsList(List.of(OrderItems.builder()
                .order(order).product(product).quantity(3).build()));

        when(orderRepository.findWithLockByOrderId(10L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(104L)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order)).thenReturn(OrderResponseDto.builder().build());

        orderServiceImpl.updateOrderStatus(10L, OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CANCELLED).build());

        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(8, product.getStockQuantity());
        verify(productRepository).save(product);
        verifyNoInteractions(paymentRepository, refundRepository);
    }

    @Test
    void updateOrderStatus_AdminCancelsConfirmedOrderFullyRefunded_ShouldNotCreateNewRefund() {
        Order order = Order.builder()
                .orderId(11L).orderNumber("ORD-TEST-011")
                .orderStatus(OrderStatus.CONFIRMED).build();
        Product product = new Product();
        product.setProductId(105L);
        product.setStockQuantity(1);
        order.setOrderItemsList(List.of(OrderItems.builder()
                .order(order).product(product).quantity(1).build()));

        Payment payment = Payment.builder()
                .paymentId(11L).order(order)
                .amount(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();
        Refund full = Refund.builder().payment(payment)
                .refundAmount(new BigDecimal("500.00")).refundStatus(RefundStatus.COMPLETED).build();

        when(orderRepository.findWithLockByOrderId(11L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.of(payment));
        when(paymentRepository.findWithLockByPaymentId(11L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentPaymentId(11L)).thenReturn(List.of(full));
        when(productRepository.findByIdWithLock(105L)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order)).thenReturn(OrderResponseDto.builder().build());

        orderServiceImpl.updateOrderStatus(11L, OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CANCELLED).build());
        verify(productRepository, times(1)).save(product);
        verify(orderRepository, times(1)).save(order);
        verify(refundRepository, never()).save(any(Refund.class));
        verify(refundRepository, never()).save(any());
        assertEquals(2, product.getStockQuantity());
    }

    @Test
    void updateOrderStatus_AdminUpdatesNonExistingOrder_ShouldThrowOrderNotFoundException() {
        when(orderRepository.findWithLockByOrderId(12L))
                .thenReturn(Optional.empty());

        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        assertThrows(OrderNotFoundException.class,
                () -> orderServiceImpl.updateOrderStatus(12L, request));

        verify(orderRepository).findWithLockByOrderId(12L);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository, paymentRepository, refundRepository, orderMapper);
    }

    @Test
    void updateOrderStatus_AdminMovesPlacedOrderDirectlyToDelivered_ShouldThrowOrderStatusException() {
        Order order = Order.builder()
                .orderId(14L)
                .orderNumber("ORD-TEST-014")
                .orderStatus(OrderStatus.PLACED)
                .build();

        when(orderRepository.findWithLockByOrderId(14L))
                .thenReturn(Optional.of(order));

        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.DELIVERED)
                .build();
        assertThrows(OrderStatusException.class,
                () -> orderServiceImpl.updateOrderStatus(14L, request));
        assertEquals(OrderStatus.PLACED, order.getOrderStatus());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(productRepository, paymentRepository, refundRepository, orderMapper);
    }

    @Test
    void updateOrderStatus_AdminSetsConfirmedOrderToConfirmed_ShouldThrowOrderStatusException() {
        Order order = Order.builder()
                .orderId(16L)
                .orderNumber("ORD-TEST-016")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findWithLockByOrderId(16L))
                .thenReturn(Optional.of(order));

        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CONFIRMED)
                .build();
        assertThrows(OrderStatusException.class,
                () -> orderServiceImpl.updateOrderStatus(16L, request));
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(productRepository, paymentRepository, refundRepository, orderMapper);
    }

    @Test
    void updateOrderStatus_AdminConfirmsNonExistingOrder_ShouldThrowOrderNotFoundException() {
        when(orderRepository.findWithLockByOrderId(18L))
                .thenReturn(Optional.empty());

        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CONFIRMED)
                .build();
        assertThrows(OrderNotFoundException.class,
                () -> orderServiceImpl.updateOrderStatus(18L, request));
        verify(orderRepository).findWithLockByOrderId(18L);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository, paymentRepository, refundRepository, orderMapper);
    }

    @Test
    void updateOrderStatus_AdminConfirmsPlacedOrder_ShouldUpdateStatus() {
        Order order = Order.builder()
                .orderId(19L)
                .orderNumber("ORD-TEST-019")
                .orderStatus(OrderStatus.PLACED)
                .build();

        when(orderRepository.findWithLockByOrderId(19L)).thenReturn(Optional.of(order));
        OrderResponseDto expectedResponse = OrderResponseDto.builder()
                .orderId(19L)
                .orderNumber("ORD-TEST-019")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order)).thenReturn(expectedResponse);

        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                        .orderStatus(OrderStatus.CONFIRMED)
                        .build();

        OrderResponseDto actualResponse = orderServiceImpl.updateOrderStatus(19L, request);
        assertEquals(OrderStatus.CONFIRMED, actualResponse.getOrderStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(orderRepository, times(1)).save(order);
        verifyNoInteractions(productRepository, paymentRepository, refundRepository);
    }

    @Test
    void updateOrderStatus_AdminMovesConfirmedOrderToShipped_ShouldUpdateStatus() {
        Order order = Order.builder()
                .orderId(20L)
                .orderNumber("ORD-TEST-020")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();
        when(orderRepository.findWithLockByOrderId(20L)).thenReturn(Optional.of(order));
        OrderResponseDto expectedResponse = OrderResponseDto.builder()
                .orderId(20L)
                .orderNumber("ORD-TEST-020")
                .orderStatus(OrderStatus.SHIPPED)
                .build();

        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order)).thenReturn(expectedResponse);
        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                        .orderStatus(OrderStatus.SHIPPED)
                        .build();
        OrderResponseDto actualResponse = orderServiceImpl.updateOrderStatus(20L, request);
        assertEquals(OrderStatus.SHIPPED, actualResponse.getOrderStatus());
        assertEquals(OrderStatus.SHIPPED, order.getOrderStatus());
        verify(orderRepository, times(1)).save(order);
        verifyNoInteractions(productRepository, paymentRepository, refundRepository);
    }

    @Test
    void updateOrderStatus_AdminMovesShippedOrderToOutForDelivery_ShouldUpdateStatus() {
        Order order = Order.builder()
                .orderId(21L)
                .orderNumber("ORD-TEST-021")
                .orderStatus(OrderStatus.SHIPPED)
                .build();

        when(orderRepository.findWithLockByOrderId(21L)).thenReturn(Optional.of(order));
        OrderResponseDto expectedResponse = OrderResponseDto.builder()
                .orderId(21L)
                .orderNumber("ORD-TEST-021")
                .orderStatus(OrderStatus.OUT_FOR_DELIVERY)
                .build();
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order)).thenReturn(expectedResponse);
        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                        .orderStatus(OrderStatus.OUT_FOR_DELIVERY)
                        .build();
        OrderResponseDto actualResponse = orderServiceImpl.updateOrderStatus(21L, request);
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, actualResponse.getOrderStatus());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order.getOrderStatus());
        verify(orderRepository, times(1)).save(order);
        verifyNoInteractions(productRepository, paymentRepository, refundRepository);
    }

    @Test
    void updateOrderStatus_AdminMovesOutForDeliveryOrderToDelivered_ShouldUpdateStatus() {
        Order order = Order.builder()
                .orderId(22L)
                .orderNumber("ORD-TEST-022")
                .orderStatus(OrderStatus.OUT_FOR_DELIVERY)
                .build();

        when(orderRepository.findWithLockByOrderId(22L)).thenReturn(Optional.of(order));
        OrderResponseDto expectedResponse = OrderResponseDto.builder()
                .orderId(22L)
                .orderNumber("ORD-TEST-022")
                .orderStatus(OrderStatus.DELIVERED)
                .build();
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order)).thenReturn(expectedResponse);
        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.DELIVERED)
                .build();
        OrderResponseDto actualResponse = orderServiceImpl.updateOrderStatus(22L, request);
        assertEquals(OrderStatus.DELIVERED, actualResponse.getOrderStatus());
        assertEquals(OrderStatus.DELIVERED, order.getOrderStatus());
        verify(orderRepository).save(order);
        verifyNoInteractions(productRepository, paymentRepository, refundRepository);
    }

    @Test
    void updateOrderStatus_AdminUpdatesDeliveredOrder_ShouldThrowOrderStatusException() {
        Order order = Order.builder()
                .orderId(23L)
                .orderNumber("ORD-TEST-023")
                .orderStatus(OrderStatus.DELIVERED)
                .build();

        when(orderRepository.findWithLockByOrderId(23L)).thenReturn(Optional.of(order));
        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        assertThrows(OrderStatusException.class,
                () -> orderServiceImpl.updateOrderStatus(23L, request));
        assertEquals(OrderStatus.DELIVERED, order.getOrderStatus());
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository, paymentRepository, refundRepository, orderMapper);
    }

    @Test
    void updateOrderStatus_AdminCancelsConfirmedOrderWithoutPayment_ShouldThrowPaymentStatusException() {
        Order order = Order.builder()
                .orderId(24L)
                .orderNumber("ORD-TEST-024")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();
        when(orderRepository.findWithLockByOrderId(24L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.empty());

        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CANCELLED)
                .build();
        assertThrows(PaymentStatusException.class,
                () -> orderServiceImpl.updateOrderStatus(24L, request));
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository, refundRepository, orderMapper);
    }

    @Test
    void updateOrderStatus_AdminCancelsConfirmedOrderWhenPaymentDisappears_ShouldThrowPaymentNotFoundException() {
        Order order = Order.builder()
                .orderId(25L)
                .orderNumber("ORD-TEST-025")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        Payment latestPayment = Payment.builder()
                .paymentId(25L)
                .order(order)
                .paymentStatus(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("500.00"))
                .build();

        when(orderRepository.findWithLockByOrderId(25L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.of(latestPayment));
        when(paymentRepository.findWithLockByPaymentId(25L)).thenReturn(Optional.empty());
        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CANCELLED)
                .build();
        assertThrows(PaymentNotFoundException.class,
                () -> orderServiceImpl.updateOrderStatus(25L, request));
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(paymentRepository).findWithLockByPaymentId(25L);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository, refundRepository, orderMapper);
    }

    @Test
    void updateOrderStatus_AdminCancelsConfirmedOrderWithPendingPayment_ShouldThrowPaymentStatusException() {
        Order order = Order.builder()
                .orderId(26L)
                .orderNumber("ORD-TEST-026")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        Payment payment = Payment.builder()
                .paymentId(26L)
                .order(order)
                .amount(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(orderRepository.findWithLockByOrderId(26L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.of(payment));
        when(paymentRepository.findWithLockByPaymentId(26L)).thenReturn(Optional.of(payment));

        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CANCELLED)
                .build();
        assertThrows(PaymentStatusException.class,
                () -> orderServiceImpl.updateOrderStatus(26L, request));
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository, refundRepository, orderMapper);
    }

    @Test
    void updateOrderStatus_AdminCancelsOverRefundedConfirmedOrder_ShouldThrowRefundException() {
        Order order = Order.builder()
                .orderId(27L)
                .orderNumber("ORD-TEST-027")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        Payment payment = Payment.builder()
                .paymentId(27L)
                .order(order)
                .amount(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        Refund existingRefund = Refund.builder()
                .refundId(27L)
                .payment(payment)
                .refundAmount(new BigDecimal("600.00"))
                .refundStatus(RefundStatus.COMPLETED)
                .build();

        when(orderRepository.findWithLockByOrderId(27L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.of(payment));
        when(paymentRepository.findWithLockByPaymentId(27L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentPaymentId(27L)).thenReturn(List.of(existingRefund));

        OrderStatusUpdateRequestDto request = OrderStatusUpdateRequestDto.builder()
                .orderStatus(OrderStatus.CANCELLED)
                .build();
        assertThrows(RefundException.class,
                () -> orderServiceImpl.updateOrderStatus(27L, request));
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(refundRepository, never()).save(any(Refund.class));
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository, orderMapper);
    }

    @Test
    void updateOrderStatus_AdminCancelsConfirmedOrderWithFullyRefundedPayment_ShouldNotCreateNewRefund() {
        Order order = Order.builder()
                .orderId(11L)
                .orderNumber("ORD-TEST-011")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        Product product = new Product();
        product.setProductId(105L);
        product.setStockQuantity(1);

        order.setOrderItemsList(List.of(OrderItems.builder()
                .order(order)
                .product(product)
                .quantity(1)
                .build()));

        Payment payment = Payment.builder()
                .paymentId(11L)
                .order(order)
                .amount(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.REFUNDED)
                .build();

        Refund fullRefund = Refund.builder()
                .payment(payment)
                .refundAmount(new BigDecimal("500.00"))
                .refundStatus(RefundStatus.COMPLETED)
                .build();
        OrderResponseDto orderResponseDto=OrderResponseDto.builder()
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findWithLockByOrderId(11L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.of(payment));
        when(paymentRepository.findWithLockByPaymentId(11L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentPaymentId(11L)).thenReturn(List.of(fullRefund));
        when(productRepository.findByIdWithLock(105L)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order))
                .thenReturn(orderResponseDto);

        OrderResponseDto response = orderServiceImpl.updateOrderStatus(
                11L,
                OrderStatusUpdateRequestDto.builder()
                        .orderStatus(OrderStatus.CANCELLED)
                        .build()
        );
        assertEquals(OrderStatus.CANCELLED, response.getOrderStatus());
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(2, product.getStockQuantity());
        verify(productRepository).save(product);
        verify(orderRepository).save(order);
        verify(refundRepository, never()).save(any(Refund.class));
    }

    @Test
    void cancelOrder_CustomerCancelsPlacedOrderWithMissingProduct_ShouldThrowProductNotFoundException() {
        User customer = User.builder()
                .userId(28L)
                .build();
        Product deletedProduct = new Product();
        deletedProduct.setProductId(108L);
        Order order = Order.builder()
                .orderId(28L)
                .orderNumber("ORD-TEST-028")
                .orderStatus(OrderStatus.PLACED)
                .user(customer)
                .build();
        order.setOrderItemsList(List.of(OrderItems.builder()
                .order(order)
                .product(deletedProduct)
                .quantity(2)
                .build()));
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(orderRepository.findWithLockByOrderId(28L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(108L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class,
                () -> orderServiceImpl.cancelOrder(28L));
        assertEquals(OrderStatus.PLACED, order.getOrderStatus());
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(paymentRepository, refundRepository, orderMapper);
    }

//    GetOrder Methods.
    @Test
    void getUserOrders_CurrentUserHasOrders_ShouldReturnMappedOrders() {
        User currentUser = User.builder()
                .userId(29L)
                .build();
        Order firstOrder = Order.builder()
                .orderId(29L)
                .orderNumber("ORD-TEST-029")
                .orderStatus(OrderStatus.PLACED)
                .user(currentUser)
                .build();
        Order secondOrder = Order.builder()
                .orderId(30L)
                .orderNumber("ORD-TEST-030")
                .orderStatus(OrderStatus.CONFIRMED)
                .user(currentUser)
                .build();
        OrderResponseDto firstResponse = OrderResponseDto.builder()
                .orderId(29L)
                .orderNumber("ORD-TEST-029")
                .orderStatus(OrderStatus.PLACED)
                .build();

        OrderResponseDto secondResponse = OrderResponseDto.builder()
                .orderId(30L)
                .orderNumber("ORD-TEST-030")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();
        when(userMapper.getCurrentUser()).thenReturn(currentUser);
        when(orderRepository.findByUser(currentUser)).thenReturn(List.of(firstOrder, secondOrder));
        when(orderMapper.mapOrderToResponse(firstOrder)).thenReturn(firstResponse);
        when(orderMapper.mapOrderToResponse(secondOrder)).thenReturn(secondResponse);
        List<OrderResponseDto> actualResponses = orderServiceImpl.getUserOrders();
        assertEquals(List.of(firstResponse, secondResponse), actualResponses);
        verify(orderRepository).findByUser(currentUser);
        verify(orderMapper).mapOrderToResponse(firstOrder);
        verify(orderMapper).mapOrderToResponse(secondOrder);
    }

    @Test
    void getUserOrders_CurrentUserHasNoOrders_ShouldReturnEmptyList() {
        User currentUser = User.builder()
                .userId(31L)
                .build();
        when(userMapper.getCurrentUser()).thenReturn(currentUser);
        when(orderRepository.findByUser(currentUser))
                .thenReturn(List.of());
        List<OrderResponseDto> actualResponses = orderServiceImpl.getUserOrders();
        assertTrue(actualResponses.isEmpty());
        verify(orderRepository).findByUser(currentUser);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void getOrderById_CustomerRequestsOwnOrder_ShouldReturnMappedOrder() {
        User customer = User.builder()
                .userId(32L)
                .build();

        Order order = Order.builder()
                .orderId(32L)
                .orderNumber("ORD-TEST-032")
                .orderStatus(OrderStatus.PLACED)
                .user(customer)
                .build();

        OrderResponseDto expectedResponse = OrderResponseDto.builder()
                .orderId(32L)
                .orderNumber("ORD-TEST-032")
                .orderStatus(OrderStatus.PLACED)
                .build();

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(orderMapper.getOrder(32L)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order)).thenReturn(expectedResponse);
        OrderResponseDto actualResponse = orderServiceImpl.getOrderById(32L);
        assertEquals(expectedResponse, actualResponse);
        verify(orderMapper).getOrder(32L);
        verify(orderMapper).mapOrderToResponse(order);
    }

    @Test
    void getOrderById_CustomerRequestsAnotherCustomersOrder_ShouldThrowOrderNotFoundException() {
        User orderOwner = User.builder()
                .userId(33L)
                .build();

        User currentUser = User.builder()
                .userId(34L)
                .build();

        Order order = Order.builder()
                .orderId(33L)
                .orderNumber("ORD-TEST-033")
                .orderStatus(OrderStatus.PLACED)
                .user(orderOwner)
                .build();

        when(userMapper.getCurrentUser()).thenReturn(currentUser);
        when(orderMapper.getOrder(33L)).thenReturn(order);
        assertThrows(OrderNotFoundException.class,
                () -> orderServiceImpl.getOrderById(33L));
        verify(orderMapper).getOrder(33L);
        verify(orderMapper, never()).mapOrderToResponse(any(Order.class));
    }

    @Test
    void getOrderById_AdminRequestsAnotherCustomersOrder_ShouldReturnMappedOrder() {
        User orderOwner = User.builder()
                .userId(35L)
                .build();

        User admin = User.builder()
                .userId(36L)
                .role(Role.ADMIN)
                .build();

        Order order = Order.builder()
                .orderId(35L)
                .orderNumber("ORD-TEST-035")
                .orderStatus(OrderStatus.CONFIRMED)
                .user(orderOwner)
                .build();

        OrderResponseDto expectedResponse = OrderResponseDto.builder()
                .orderId(35L)
                .orderNumber("ORD-TEST-035")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        when(userMapper.getCurrentUser()).thenReturn(admin);
        when(orderMapper.getOrder(35L)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order)).thenReturn(expectedResponse);
        OrderResponseDto actualResponse = orderServiceImpl.getOrderById(35L);
        assertEquals(expectedResponse, actualResponse);
        verify(orderMapper).getOrder(35L);
        verify(orderMapper).mapOrderToResponse(order);
    }

    @Test
    void getOrderById_OrderDoesNotExist_ShouldThrowOrderNotFoundException() {
        User currentUser = User.builder()
                .userId(37L)
                .build();
        when(userMapper.getCurrentUser()).thenReturn(currentUser);
        when(orderMapper.getOrder(37L))
                .thenThrow(new OrderNotFoundException("Order not found for Id: 37"));
        assertThrows(OrderNotFoundException.class,
                () -> orderServiceImpl.getOrderById(37L));
        verify(orderMapper).getOrder(37L);
        verify(orderMapper, never()).mapOrderToResponse(any(Order.class));
    }

    @Test
    void getAllOrders_OrdersExist_ShouldReturnMappedOrders() {
        Order firstOrder = Order.builder()
                .orderId(38L)
                .orderNumber("ORD-TEST-038")
                .orderStatus(OrderStatus.PLACED)
                .build();

        Order secondOrder = Order.builder()
                .orderId(39L)
                .orderNumber("ORD-TEST-039")
                .orderStatus(OrderStatus.DELIVERED)
                .build();

        OrderResponseDto firstResponse = OrderResponseDto.builder()
                .orderId(38L)
                .orderNumber("ORD-TEST-038")
                .orderStatus(OrderStatus.PLACED)
                .build();

        OrderResponseDto secondResponse = OrderResponseDto.builder()
                .orderId(39L)
                .orderNumber("ORD-TEST-039")
                .orderStatus(OrderStatus.DELIVERED)
                .build();

        when(orderRepository.findAll()).thenReturn(List.of(firstOrder, secondOrder));
        when(orderMapper.mapOrderToResponse(firstOrder)).thenReturn(firstResponse);
        when(orderMapper.mapOrderToResponse(secondOrder)).thenReturn(secondResponse);
        List<OrderResponseDto> actualResponses = orderServiceImpl.getAllOrders();
        assertEquals(List.of(firstResponse, secondResponse), actualResponses);
        verify(orderRepository).findAll();
        verify(orderMapper).mapOrderToResponse(firstOrder);
        verify(orderMapper).mapOrderToResponse(secondOrder);
    }

    @Test
    void getAllOrders_NoOrdersExist_ShouldReturnEmptyList() {
        when(orderRepository.findAll()).thenReturn(List.of());
        List<OrderResponseDto> actualResponses = orderServiceImpl.getAllOrders();
        assertTrue(actualResponses.isEmpty());
        verify(orderRepository).findAll();
        verifyNoInteractions(orderMapper);
    }

// PlaceOrder Methods.
    @Test
    void placeOrder_EmptyCart_ShouldThrowIllegalArgumentException() {
        User customer = User.builder()
                .userId(40L)
                .build();
        Cart emptyCart = new Cart();
        emptyCart.setUser(customer);
        emptyCart.setTotalAmount(BigDecimal.ZERO);
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(emptyCart);
        assertThrows(IllegalArgumentException.class,
                () -> orderServiceImpl.placeOrder(null));
        verify(cartMapper).getCartForUser(customer);
        verifyNoInteractions(couponRepository, productRepository, orderRepository, cartRepository,
                paymentRepository, refundRepository, orderMapper);
    }

    @Test
    void placeOrder_ValidCartWithoutCoupon_ShouldCreateOrderDeductStockAndClearCart() {
        User customer = User.builder()
                .userId(41L)
                .build();

        Product product = new Product();
        product.setProductId(109L);
        product.setActive(true);
        product.setStockQuantity(10);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItem.setPriceAtAdded(new BigDecimal("250.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));
        OrderResponseDto expectedResponse = OrderResponseDto.builder()
                .orderStatus(OrderStatus.PLACED)
                .build();

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(productRepository.findByIdWithLock(109L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.mapOrderToResponse(any(Order.class))).thenReturn(expectedResponse);

        OrderResponseDto actualResponse = orderServiceImpl.placeOrder(null);
        assertEquals(expectedResponse, actualResponse);
        assertEquals(8, product.getStockQuantity());
        assertTrue(cart.getCartItemsList().isEmpty());
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(customer, savedOrder.getUser());
        assertEquals(OrderStatus.PLACED, savedOrder.getOrderStatus());
        assertNull(savedOrder.getCouponCode());
        assertEquals(0, new BigDecimal("0.00")
                .compareTo(savedOrder.getDiscountAmount()));
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(savedOrder.getTotalOrderAmount()));
        assertEquals(1, savedOrder.getOrderItemsList().size());
        verify(productRepository).save(product);
        verify(cartMapper).calculateTotalCartAmount(cart);
        verify(cartRepository).save(cart);
        verifyNoInteractions(couponRepository, paymentRepository, refundRepository);
    }

    @Test
    void placeOrder_ValidCartWithCoupon_ShouldApplyDiscountAndIncrementCouponUsage() {
        User customer = User.builder()
                .userId(42L)
                .build();

        Product product = new Product();
        product.setProductId(110L);
        product.setActive(true);
        product.setStockQuantity(5);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        Coupon coupon = new Coupon();
        coupon.setCouponCode("SAVE10");
        coupon.setActiveStatus(true);
        coupon.setExpiryDate(LocalDateTime.now().plusDays(1));
        coupon.setUsageCount(2);
        coupon.setUsageLimit(10);
        coupon.setDiscountPercentage(new BigDecimal("10.00"));
        coupon.setMinimumOrderAmount(new BigDecimal("100.00"));

        OrderResponseDto expectedResponse = OrderResponseDto.builder()
                .orderStatus(OrderStatus.PLACED)
                .build();
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(couponRepository.findByCouponCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(productRepository.findByIdWithLock(110L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.mapOrderToResponse(any(Order.class))).thenReturn(expectedResponse);
        OrderResponseDto actualResponse = orderServiceImpl.placeOrder(" save10 ");
        assertEquals(expectedResponse, actualResponse);
        assertEquals(3, coupon.getUsageCount());
        assertEquals(4, product.getStockQuantity());
        assertTrue(cart.getCartItemsList().isEmpty());
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals("SAVE10", savedOrder.getCouponCode());
        assertEquals(0, new BigDecimal("50.00")
                .compareTo(savedOrder.getDiscountAmount()));
        assertEquals(0, new BigDecimal("450.00")
                .compareTo(savedOrder.getTotalOrderAmount()));
        verify(couponRepository).save(coupon);
        verify(productRepository).save(product);
        verify(cartMapper).calculateTotalCartAmount(cart);
        verify(cartRepository).save(cart);
    }

    @Test
    void placeOrder_InvalidCouponCode_ShouldThrowCouponNotFoundException() {
        User customer = User.builder()
                .userId(43L)
                .build();

        Product product = new Product();
        product.setProductId(111L);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(couponRepository.findByCouponCode("INVALID")).thenReturn(Optional.empty());
        assertThrows(CouponNotFoundException.class,
                () -> orderServiceImpl.placeOrder("invalid"));
        assertEquals(1, cart.getCartItemsList().size());
        verify(couponRepository).findByCouponCode("INVALID");
        verifyNoInteractions(productRepository, orderRepository, cartRepository, paymentRepository,
                refundRepository, orderMapper);
    }

    @Test
    void placeOrder_InactiveCoupon_ShouldThrowCouponException() {
        User customer = User.builder()
                .userId(44L)
                .build();

        Product product = new Product();
        product.setProductId(112L);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        Coupon inactiveCoupon = new Coupon();
        inactiveCoupon.setCouponCode("INACTIVE10");
        inactiveCoupon.setActiveStatus(false);

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(couponRepository.findByCouponCode("INACTIVE10")).thenReturn(Optional.of(inactiveCoupon));
        assertThrows(CouponException.class,
                () -> orderServiceImpl.placeOrder("inactive10"));
        assertEquals(1, cart.getCartItemsList().size());
        verify(couponRepository, never()).save(any(Coupon.class));
        verifyNoInteractions(productRepository, orderRepository, cartRepository, paymentRepository,
                refundRepository, orderMapper);
    }

    @Test
    void placeOrder_ExpiredCoupon_ShouldThrowCouponException() {
        User customer = User.builder()
                .userId(45L)
                .build();

        Product product = new Product();
        product.setProductId(113L);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        Coupon expiredCoupon = new Coupon();
        expiredCoupon.setCouponCode("EXPIRED10");
        expiredCoupon.setActiveStatus(true);
        expiredCoupon.setExpiryDate(LocalDateTime.now().minusDays(1));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(couponRepository.findByCouponCode("EXPIRED10")).thenReturn(Optional.of(expiredCoupon));
        assertThrows(CouponException.class,
                () -> orderServiceImpl.placeOrder("expired10"));
        assertEquals(1, cart.getCartItemsList().size());
        verify(couponRepository, never()).save(any(Coupon.class));
        verifyNoInteractions(productRepository, orderRepository, cartRepository, paymentRepository,
                refundRepository, orderMapper);
    }

    @Test
    void placeOrder_CouponUsageLimitReached_ShouldThrowCouponException() {
        User customer = User.builder()
                .userId(46L)
                .build();

        Product product = new Product();
        product.setProductId(114L);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        Coupon exhaustedCoupon = new Coupon();
        exhaustedCoupon.setCouponCode("LIMIT10");
        exhaustedCoupon.setActiveStatus(true);
        exhaustedCoupon.setExpiryDate(LocalDateTime.now().plusDays(1));
        exhaustedCoupon.setUsageCount(10);
        exhaustedCoupon.setUsageLimit(10);

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(couponRepository.findByCouponCode("LIMIT10")).thenReturn(Optional.of(exhaustedCoupon));
        assertThrows(CouponException.class,
                () -> orderServiceImpl.placeOrder("limit10"));
        assertEquals(10, exhaustedCoupon.getUsageCount());
        assertEquals(1, cart.getCartItemsList().size());
        verify(couponRepository, never()).save(any(Coupon.class));
        verifyNoInteractions(productRepository, orderRepository, cartRepository, paymentRepository,
                refundRepository, orderMapper);
    }

    @Test
    void placeOrder_CartBelowCouponMinimumAmount_ShouldThrowCouponException() {
        User customer = User.builder()
                .userId(47L)
                .build();

        Product product = new Product();
        product.setProductId(115L);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        Coupon coupon = new Coupon();
        coupon.setCouponCode("MIN600");
        coupon.setActiveStatus(true);
        coupon.setExpiryDate(LocalDateTime.now().plusDays(1));
        coupon.setMinimumOrderAmount(new BigDecimal("600.00"));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(couponRepository.findByCouponCode("MIN600")).thenReturn(Optional.of(coupon));
        assertThrows(CouponException.class,
                () -> orderServiceImpl.placeOrder("min600"));
        assertEquals(1, cart.getCartItemsList().size());
        verify(couponRepository, never()).save(any(Coupon.class));
        verifyNoInteractions(productRepository, orderRepository, cartRepository, paymentRepository,
                refundRepository, orderMapper);
    }

    @Test
    void placeOrder_RequestedQuantityExceedsStock_ShouldThrowIllegalArgumentException() {
        User customer = User.builder()
                .userId(48L)
                .build();

        Product product = new Product();
        product.setProductId(116L);
        product.setActive(true);
        product.setStockQuantity(1);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItem.setPriceAtAdded(new BigDecimal("250.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(productRepository.findByIdWithLock(116L)).thenReturn(Optional.of(product));
        assertThrows(IllegalArgumentException.class,
                () -> orderServiceImpl.placeOrder(null));
        assertEquals(1, product.getStockQuantity());
        assertEquals(1, cart.getCartItemsList().size());
        verify(productRepository).findByIdWithLock(116L);
        verify(productRepository, never()).save(any(Product.class));
        verifyNoInteractions(couponRepository, orderRepository, cartRepository,
                paymentRepository, refundRepository, orderMapper);
    }

    @Test
    void placeOrder_InactiveProduct_ShouldThrowIllegalArgumentException() {
        User customer = User.builder()
                .userId(49L)
                .build();

        Product inactiveProduct = new Product();
        inactiveProduct.setProductId(117L);
        inactiveProduct.setActive(false);
        inactiveProduct.setStockQuantity(10);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(inactiveProduct);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(productRepository.findByIdWithLock(117L)).thenReturn(Optional.of(inactiveProduct));
        assertThrows(IllegalArgumentException.class,
                () -> orderServiceImpl.placeOrder(null));
        assertEquals(10, inactiveProduct.getStockQuantity());
        assertEquals(1, cart.getCartItemsList().size());
        verify(productRepository, never()).save(any(Product.class));
        verifyNoInteractions(couponRepository, orderRepository, cartRepository, paymentRepository,
                refundRepository, orderMapper);
    }

    @Test
    void placeOrder_ProductDoesNotExist_ShouldThrowProductNotFoundException() {
        User customer = User.builder()
                .userId(50L)
                .build();

        Product missingProductReference = new Product();
        missingProductReference.setProductId(118L);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(missingProductReference);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(productRepository.findByIdWithLock(118L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class,
                () -> orderServiceImpl.placeOrder(null));
        assertEquals(1, cart.getCartItemsList().size());
        verify(productRepository, never()).save(any(Product.class));
        verifyNoInteractions(couponRepository, orderRepository, cartRepository, paymentRepository,
                refundRepository, orderMapper);
    }

    @Test
    void placeOrder_BlankCouponCode_ShouldCreateOrderWithoutCoupon() {
        User customer = User.builder()
                .userId(51L)
                .build();

        Product product = new Product();
        product.setProductId(119L);
        product.setActive(true);
        product.setStockQuantity(3);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        OrderResponseDto expectedResponse = OrderResponseDto.builder()
                .orderStatus(OrderStatus.PLACED)
                .build();

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(productRepository.findByIdWithLock(119L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.mapOrderToResponse(any(Order.class))).thenReturn(expectedResponse);

        OrderResponseDto actualResponse = orderServiceImpl.placeOrder("   ");
        assertEquals(expectedResponse, actualResponse);
        assertEquals(2, product.getStockQuantity());
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertNull(savedOrder.getCouponCode());
        assertEquals(0, BigDecimal.ZERO.compareTo(savedOrder.getDiscountAmount()));
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(savedOrder.getTotalOrderAmount()));
        verifyNoInteractions(couponRepository);
        verify(productRepository).save(product);
        verify(cartRepository).save(cart);
    }

    @Test
    void placeOrder_CartTotalIsNull_ShouldThrowIllegalArgumentException() {
        User customer = User.builder()
                .userId(52L)
                .build();

        Product product = new Product();
        product.setProductId(120L);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(null);

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        assertThrows(IllegalArgumentException.class,
                () -> orderServiceImpl.placeOrder(null));
        assertEquals(1, cart.getCartItemsList().size());
        verifyNoInteractions(couponRepository, productRepository, orderRepository, cartRepository,
                paymentRepository, refundRepository, orderMapper);
    }

    @Test
    void placeOrder_CartTotalIsZero_ShouldThrowIllegalArgumentException() {
        User customer = User.builder()
                .userId(53L)
                .build();

        Product product = new Product();
        product.setProductId(121L);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(BigDecimal.ZERO);

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(BigDecimal.ZERO);
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        assertThrows(IllegalArgumentException.class,
                () -> orderServiceImpl.placeOrder(null));
        assertEquals(1, cart.getCartItemsList().size());
        verifyNoInteractions(couponRepository, productRepository, orderRepository, cartRepository,
                paymentRepository, refundRepository, orderMapper);
    }

    @Test
    void placeOrder_MultipleProducts_ShouldLockAndDeductStockInProductIdOrder() {
        User customer = User.builder()
                .userId(54L)
                .build();

        Product lowerIdProduct = new Product();
        lowerIdProduct.setProductId(122L);
        lowerIdProduct.setActive(true);
        lowerIdProduct.setStockQuantity(10);

        Product higherIdProduct = new Product();
        higherIdProduct.setProductId(123L);
        higherIdProduct.setActive(true);
        higherIdProduct.setStockQuantity(10);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems higherIdItem = new CartItems();
        higherIdItem.setCart(cart);
        higherIdItem.setProduct(higherIdProduct);
        higherIdItem.setQuantity(1);
        higherIdItem.setPriceAtAdded(new BigDecimal("300.00"));

        CartItems lowerIdItem = new CartItems();
        lowerIdItem.setCart(cart);
        lowerIdItem.setProduct(lowerIdProduct);
        lowerIdItem.setQuantity(1);
        lowerIdItem.setPriceAtAdded(new BigDecimal("200.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(higherIdItem, lowerIdItem)));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(productRepository.findByIdWithLock(122L))
                .thenReturn(Optional.of(lowerIdProduct));
        when(productRepository.findByIdWithLock(123L))
                .thenReturn(Optional.of(higherIdProduct));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.mapOrderToResponse(any(Order.class)))
                .thenReturn(OrderResponseDto.builder()
                        .orderStatus(OrderStatus.PLACED)
                        .build());

        orderServiceImpl.placeOrder(null);
        assertEquals(9, lowerIdProduct.getStockQuantity());
        assertEquals(9, higherIdProduct.getStockQuantity());

        InOrder productCalls = inOrder(productRepository);
        productCalls.verify(productRepository).findByIdWithLock(122L);
        productCalls.verify(productRepository).save(lowerIdProduct);
        productCalls.verify(productRepository).findByIdWithLock(123L);
        productCalls.verify(productRepository).save(higherIdProduct);
    }

    @Test
    void placeOrder_CouponWithoutExpiryDate_ShouldThrowCouponException() {
        User customer = User.builder()
                .userId(55L)
                .build();

        Product product = new Product();
        product.setProductId(124L);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        Coupon coupon = new Coupon();
        coupon.setCouponCode("NOEXPIRY");
        coupon.setActiveStatus(true);
        coupon.setExpiryDate(null);

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(couponRepository.findByCouponCode("NOEXPIRY")).thenReturn(Optional.of(coupon));
        assertThrows(CouponException.class,
                () -> orderServiceImpl.placeOrder("noexpiry"));
        verify(couponRepository, never()).save(any(Coupon.class));
        verifyNoInteractions(productRepository, orderRepository, cartRepository,
                paymentRepository, refundRepository, orderMapper);
    }

    @Test
    void placeOrder_CartAtCouponMinimumAmount_ShouldApplyCoupon() {
        User customer = User.builder()
                .userId(56L)
                .build();

        Product product = new Product();
        product.setProductId(125L);
        product.setActive(true);
        product.setStockQuantity(2);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        Coupon coupon = new Coupon();
        coupon.setCouponCode("MIN500");
        coupon.setActiveStatus(true);
        coupon.setExpiryDate(LocalDateTime.now().plusDays(1));
        coupon.setUsageCount(0);
        coupon.setUsageLimit(10);
        coupon.setDiscountPercentage(new BigDecimal("10.00"));
        coupon.setMinimumOrderAmount(new BigDecimal("500.00"));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(couponRepository.findByCouponCode("MIN500")).thenReturn(Optional.of(coupon));
        when(productRepository.findByIdWithLock(125L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.mapOrderToResponse(any(Order.class)))
                .thenReturn(OrderResponseDto.builder()
                        .orderStatus(OrderStatus.PLACED)
                        .build());

        orderServiceImpl.placeOrder("min500");
        assertEquals(1, coupon.getUsageCount());
        assertEquals(1, product.getStockQuantity());
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(0, new BigDecimal("50.00")
                .compareTo(savedOrder.getDiscountAmount()));
        assertEquals(0, new BigDecimal("450.00")
                .compareTo(savedOrder.getTotalOrderAmount()));
        verify(couponRepository).save(coupon);
    }

    @Test
    void placeOrder_CouponDiscountRequiresRounding_ShouldUseHalfUpRounding() {
        User customer = User.builder()
                .userId(57L)
                .build();

        Product product = new Product();
        product.setProductId(126L);
        product.setActive(true);
        product.setStockQuantity(1);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("100.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("100.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        Coupon coupon = new Coupon();
        coupon.setCouponCode("ROUND");
        coupon.setActiveStatus(true);
        coupon.setExpiryDate(LocalDateTime.now().plusDays(1));
        coupon.setUsageCount(0);
        coupon.setUsageLimit(10);
        coupon.setDiscountPercentage(new BigDecimal("12.345"));
        coupon.setMinimumOrderAmount(new BigDecimal("100.00"));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(couponRepository.findByCouponCode("ROUND")).thenReturn(Optional.of(coupon));
        when(productRepository.findByIdWithLock(126L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.mapOrderToResponse(any(Order.class)))
                .thenReturn(OrderResponseDto.builder()
                        .orderStatus(OrderStatus.PLACED)
                        .build());

        orderServiceImpl.placeOrder("round");
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(0, new BigDecimal("12.35").compareTo(savedOrder.getDiscountAmount()));
        assertEquals(0, new BigDecimal("87.65").compareTo(savedOrder.getTotalOrderAmount()));
    }

    @Test
    void placeOrder_CouponWithoutUsageLimit_ShouldApplyCoupon() {
        User customer = User.builder()
                .userId(58L)
                .build();

        Product product = new Product();
        product.setProductId(127L);
        product.setActive(true);
        product.setStockQuantity(2);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("200.00"));

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("200.00"));
        cart.setCartItemsList(new ArrayList<>(List.of(cartItem)));

        Coupon coupon = new Coupon();
        coupon.setCouponCode("UNLIMITED");
        coupon.setActiveStatus(true);
        coupon.setExpiryDate(LocalDateTime.now().plusDays(1));
        coupon.setUsageCount(99);
        coupon.setUsageLimit(null);
        coupon.setDiscountPercentage(new BigDecimal("10.00"));
        coupon.setMinimumOrderAmount(new BigDecimal("100.00"));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(cartMapper.getCartForUser(customer)).thenReturn(cart);
        when(couponRepository.findByCouponCode("UNLIMITED")).thenReturn(Optional.of(coupon));
        when(productRepository.findByIdWithLock(127L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.mapOrderToResponse(any(Order.class)))
                .thenReturn(OrderResponseDto.builder()
                        .orderStatus(OrderStatus.PLACED)
                        .build());

        orderServiceImpl.placeOrder("unlimited");
        assertEquals(100, coupon.getUsageCount());
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals("UNLIMITED", savedOrder.getCouponCode());
        assertEquals(0, new BigDecimal("20.00").compareTo(savedOrder.getDiscountAmount()));
        assertEquals(0, new BigDecimal("180.00").compareTo(savedOrder.getTotalOrderAmount()));
        verify(couponRepository).save(coupon);
    }

    @Test
    void cancelOrder_MultipleProducts_ShouldRestoreStockInProductIdOrder() {
        User customer = User.builder()
                .userId(59L)
                .build();

        Product lowerIdProduct = new Product();
        lowerIdProduct.setProductId(128L);
        lowerIdProduct.setStockQuantity(5);

        Product higherIdProduct = new Product();
        higherIdProduct.setProductId(129L);
        higherIdProduct.setStockQuantity(7);

        Order order = Order.builder()
                .orderId(59L)
                .orderNumber("ORD-TEST-059")
                .orderStatus(OrderStatus.PLACED)
                .user(customer)
                .build();

        OrderItems higherIdItem = OrderItems.builder()
                .order(order)
                .product(higherIdProduct)
                .quantity(2)
                .build();

        OrderItems lowerIdItem = OrderItems.builder()
                .order(order)
                .product(lowerIdProduct)
                .quantity(3)
                .build();

        order.setOrderItemsList(List.of(higherIdItem, lowerIdItem));

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(orderRepository.findWithLockByOrderId(59L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(128L)).thenReturn(Optional.of(lowerIdProduct));
        when(productRepository.findByIdWithLock(129L)).thenReturn(Optional.of(higherIdProduct));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order))
                .thenReturn(OrderResponseDto.builder()
                        .orderStatus(OrderStatus.CANCELLED)
                        .build());

        orderServiceImpl.cancelOrder(59L);
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(8, lowerIdProduct.getStockQuantity());
        assertEquals(9, higherIdProduct.getStockQuantity());
        InOrder productCalls = inOrder(productRepository);
        productCalls.verify(productRepository).findByIdWithLock(128L);
        productCalls.verify(productRepository).save(lowerIdProduct);
        productCalls.verify(productRepository).findByIdWithLock(129L);
        productCalls.verify(productRepository).save(higherIdProduct);
        verify(orderRepository).save(order);
        verifyNoInteractions(paymentRepository, refundRepository);
    }

    @Test
    void updateOrderStatus_AdminCancelsOrderWithFullPendingRefund_ShouldNotCreateAnotherRefund() {
        Order order = Order.builder()
                .orderId(60L)
                .orderNumber("ORD-TEST-060")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        Product product = new Product();
        product.setProductId(130L);
        product.setStockQuantity(4);

        order.setOrderItemsList(List.of(OrderItems.builder()
                .order(order)
                .product(product)
                .quantity(1)
                .build()));

        Payment payment = Payment.builder()
                .paymentId(60L)
                .order(order)
                .amount(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        Refund pendingRefund = Refund.builder()
                .payment(payment)
                .refundAmount(new BigDecimal("500.00"))
                .refundStatus(RefundStatus.PENDING)
                .build();

        when(orderRepository.findWithLockByOrderId(60L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.of(payment));
        when(paymentRepository.findWithLockByPaymentId(60L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentPaymentId(60L)).thenReturn(List.of(pendingRefund));
        when(productRepository.findByIdWithLock(130L)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order))
                .thenReturn(OrderResponseDto.builder()
                        .orderStatus(OrderStatus.CANCELLED)
                        .build());
        orderServiceImpl.updateOrderStatus(
                60L,
                OrderStatusUpdateRequestDto.builder()
                        .orderStatus(OrderStatus.CANCELLED)
                        .build()
        );
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(5, product.getStockQuantity());
        verify(refundRepository, never()).save(any(Refund.class));
        verify(productRepository).save(product);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_AdminCancelsOrderWithFailedRefund_ShouldCreateFullReplacementRefund() {
        Order order = Order.builder()
                .orderId(61L)
                .orderNumber("ORD-TEST-061")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        Product product = new Product();
        product.setProductId(131L);
        product.setStockQuantity(4);

        order.setOrderItemsList(List.of(OrderItems.builder()
                .order(order)
                .product(product)
                .quantity(1)
                .build()));

        Payment payment = Payment.builder()
                .paymentId(61L)
                .order(order)
                .amount(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        Refund failedRefund = Refund.builder()
                .payment(payment)
                .refundAmount(new BigDecimal("500.00"))
                .refundStatus(RefundStatus.FAILED)
                .build();

        when(orderRepository.findWithLockByOrderId(61L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.of(payment));
        when(paymentRepository.findWithLockByPaymentId(61L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentPaymentId(61L)).thenReturn(List.of(failedRefund));
        when(productRepository.findByIdWithLock(131L)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.mapOrderToResponse(order))
                .thenReturn(OrderResponseDto.builder()
                        .orderStatus(OrderStatus.CANCELLED)
                        .build());
        orderServiceImpl.updateOrderStatus(
                61L,
                OrderStatusUpdateRequestDto.builder()
                        .orderStatus(OrderStatus.CANCELLED)
                        .build()
        );
        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(refundCaptor.capture());
        Refund replacementRefund = refundCaptor.getValue();
        assertEquals(0, new BigDecimal("500.00").compareTo(replacementRefund.getRefundAmount()));
        assertEquals(RefundStatus.PENDING, replacementRefund.getRefundStatus());
        assertSame(payment, replacementRefund.getPayment());
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(5, product.getStockQuantity());
    }
}
