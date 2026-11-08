package com.shopsphere.service.serviceImpl;

import com.shopsphere.constant.OrderStatus;
import com.shopsphere.constant.PaymentMethod;
import com.shopsphere.constant.PaymentStatus;
import com.shopsphere.dto.PaymentRequestDto;
import com.shopsphere.dto.PaymentResponseDto;
import com.shopsphere.entity.Order;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.User;
import com.shopsphere.exception.AccessDeniedException;
import com.shopsphere.exception.OrderNotFoundException;
import com.shopsphere.exception.PaymentStatusException;
import com.shopsphere.mapper.PaymentMapper;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentServiceImpl;

    @Test
    void createPayment_CustomerPaysForAnotherCustomersOrder_ShouldThrowAccessDeniedException() {
        User orderOwner = User.builder()
                .userId(2L)
                .build();

        User currentUser = User.builder()
                .userId(3L)
                .build();

        Order order = Order.builder()
                .orderId(2L)
                .orderNumber("ORD-PAY-002")
                .orderStatus(OrderStatus.PLACED)
                .totalOrderAmount(new BigDecimal("500.00"))
                .user(orderOwner)
                .build();

        PaymentRequestDto request = PaymentRequestDto.builder()
                .paymentMethod(PaymentMethod.UPI)
                .build();
        when(userMapper.getCurrentUser()).thenReturn(currentUser);
        when(orderRepository.findWithLockByOrderId(2L)).thenReturn(Optional.of(order));
        assertThrows(AccessDeniedException.class,
                () -> paymentServiceImpl.createPayment(2L, request));
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(paymentMapper);
    }

    @Test
    void createPayment_OrderDoesNotExist_ShouldThrowOrderNotFoundException() {
        User customer = User.builder()
                .userId(4L)
                .build();

        PaymentRequestDto request = PaymentRequestDto.builder()
                .paymentMethod(PaymentMethod.CARD)
                .build();
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(orderRepository.findWithLockByOrderId(4L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class,
                () -> paymentServiceImpl.createPayment(4L, request));
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(paymentMapper);
    }

    @Test
    void createPayment_ConfirmedOrder_ShouldThrowPaymentStatusException() {
        User customer = User.builder()
                .userId(5L)
                .build();

        Order order = Order.builder()
                .orderId(5L)
                .orderNumber("ORD-PAY-005")
                .orderStatus(OrderStatus.CONFIRMED)
                .totalOrderAmount(new BigDecimal("500.00"))
                .user(customer)
                .build();

        PaymentRequestDto request = PaymentRequestDto.builder()
                .paymentMethod(PaymentMethod.CARD)
                .build();
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(orderRepository.findWithLockByOrderId(5L)).thenReturn(Optional.of(order));
        assertThrows(PaymentStatusException.class,
                () -> paymentServiceImpl.createPayment(5L, request));
        verifyNoInteractions(paymentRepository, paymentMapper);
    }

    @Test
    void createPayment_OrderHasPendingPayment_ShouldThrowPaymentStatusException() {
        User customer = User.builder()
                .userId(6L)
                .build();

        Order order = Order.builder()
                .orderId(6L)
                .orderNumber("ORD-PAY-006")
                .orderStatus(OrderStatus.PLACED)
                .totalOrderAmount(new BigDecimal("500.00"))
                .user(customer)
                .build();

        Payment pendingPayment = Payment.builder()
                .paymentId(6L)
                .order(order)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        PaymentRequestDto request = PaymentRequestDto.builder()
                .paymentMethod(PaymentMethod.UPI)
                .build();
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(orderRepository.findWithLockByOrderId(6L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.of(pendingPayment));
        assertThrows(PaymentStatusException.class,
                () -> paymentServiceImpl.createPayment(6L, request));
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(paymentMapper);
    }

    @Test
    void createPayment_OrderHasSuccessfulPayment_ShouldThrowPaymentStatusException() {
        User customer = User.builder()
                .userId(7L)
                .build();

        Order order = Order.builder()
                .orderId(7L)
                .orderNumber("ORD-PAY-007")
                .orderStatus(OrderStatus.PLACED)
                .totalOrderAmount(new BigDecimal("500.00"))
                .user(customer)
                .build();

        Payment successfulPayment = Payment.builder()
                .paymentId(7L)
                .order(order)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        PaymentRequestDto request = PaymentRequestDto.builder()
                .paymentMethod(PaymentMethod.UPI)
                .build();
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(orderRepository.findWithLockByOrderId(7L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.of(successfulPayment));
        assertThrows(PaymentStatusException.class,
                () -> paymentServiceImpl.createPayment(7L, request));
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(paymentMapper);
    }

    @Test
    void createPayment_OrderHasFailedPayment_ShouldCreateNewPendingPayment() {
        User customer = User.builder()
                .userId(8L)
                .build();

        Order order = Order.builder()
                .orderId(8L)
                .orderNumber("ORD-PAY-008")
                .orderStatus(OrderStatus.PLACED)
                .totalOrderAmount(new BigDecimal("500.00"))
                .user(customer)
                .build();

        Payment failedPayment = Payment.builder()
                .paymentId(8L)
                .order(order)
                .paymentStatus(PaymentStatus.FAILED)
                .build();

        PaymentRequestDto request = PaymentRequestDto.builder()
                .paymentMethod(PaymentMethod.CARD)
                .build();

        PaymentResponseDto expectedResponse = PaymentResponseDto.builder()
                .build();

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(orderRepository.findWithLockByOrderId(8L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.of(failedPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.mapPaymentToResponse(any(Payment.class))).thenReturn(expectedResponse);
        PaymentResponseDto actualResponse =
                paymentServiceImpl.createPayment(8L, request);
        assertEquals(expectedResponse, actualResponse);
        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment newPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.PENDING, newPayment.getPaymentStatus());
        assertEquals(PaymentMethod.CARD, newPayment.getPaymentMethod());
        assertEquals(order, newPayment.getOrder());
        assertEquals(customer, newPayment.getUser());
        assertEquals(0, new BigDecimal("500.00").compareTo(newPayment.getAmount()));
    }

    @Test
    void successPayment_PendingPaymentForPlacedOrder_ShouldMarkPaymentSuccessfulAndConfirmOrder() {
        Order order = Order.builder()
                .orderId(9L)
                .orderNumber("ORD-PAY-009")
                .orderStatus(OrderStatus.PLACED)
                .build();

        Payment payment = Payment.builder()
                .paymentId(9L)
                .order(order)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        PaymentResponseDto expectedResponse = PaymentResponseDto.builder()
                .build();

        when(paymentMapper.getPayment(9L)).thenReturn(payment);
        when(orderRepository.findWithLockByOrderId(9L)).thenReturn(Optional.of(order));
        when(paymentMapper.getPaymentWithLock(9L)).thenReturn(payment);
        when(orderRepository.save(order)).thenReturn(order);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.mapPaymentToResponse(payment)).thenReturn(expectedResponse);

        PaymentResponseDto actualResponse = paymentServiceImpl.successPayment(9L);
        assertEquals(expectedResponse, actualResponse);
        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());
        assertNotNull(payment.getPaidAt());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(orderRepository).save(order);
        verify(paymentRepository).save(payment);
        verify(paymentMapper).getPayment(9L);
        verify(paymentMapper).getPaymentWithLock(9L);
    }

    @Test
    void successPayment_AlreadySuccessfulPayment_ShouldThrowPaymentStatusException() {
        Order order = Order.builder()
                .orderId(10L)
                .orderNumber("ORD-PAY-010")
                .orderStatus(OrderStatus.PLACED)
                .build();

        Payment payment = Payment.builder()
                .paymentId(10L)
                .order(order)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        when(paymentMapper.getPayment(10L)).thenReturn(payment);
        when(orderRepository.findWithLockByOrderId(10L)).thenReturn(Optional.of(order));
        when(paymentMapper.getPaymentWithLock(10L)).thenReturn(payment);
        assertThrows(PaymentStatusException.class,
                () -> paymentServiceImpl.successPayment(10L));
        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());
        assertEquals(OrderStatus.PLACED, order.getOrderStatus());
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentMapper, never()).mapPaymentToResponse(any(Payment.class));
    }

    @Test
    void successPayment_CancelledOrder_ShouldThrowPaymentStatusException() {
        Order order = Order.builder()
                .orderId(11L)
                .orderNumber("ORD-PAY-011")
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        Payment payment = Payment.builder()
                .paymentId(11L)
                .order(order)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        when(paymentMapper.getPayment(11L)).thenReturn(payment);
        when(orderRepository.findWithLockByOrderId(11L)).thenReturn(Optional.of(order));
        when(paymentMapper.getPaymentWithLock(11L)).thenReturn(payment);
        assertThrows(PaymentStatusException.class,
                () -> paymentServiceImpl.successPayment(11L));
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentMapper, never()).mapPaymentToResponse(any(Payment.class));
    }

    @Test
    void createPayment_PlacedOrderOwnedByCustomer_ShouldCreatePendingPayment() {
        User customer = User.builder()
                .userId(1L)
                .build();

        Order order = Order.builder()
                .orderId(1L)
                .orderNumber("ORD-PAY-001")
                .orderStatus(OrderStatus.PLACED)
                .totalOrderAmount(new BigDecimal("500.00"))
                .user(customer)
                .build();

        PaymentRequestDto request = PaymentRequestDto.builder()
                .paymentMethod(PaymentMethod.UPI)
                .build();

        PaymentResponseDto expectedResponse = PaymentResponseDto.builder()
                .build();

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(orderRepository.findWithLockByOrderId(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.mapPaymentToResponse(any(Payment.class))).thenReturn(expectedResponse);

        PaymentResponseDto actualResponse =
                paymentServiceImpl.createPayment(1L, request);
        assertEquals(expectedResponse, actualResponse);
        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertSame(order, savedPayment.getOrder());
        assertSame(customer, savedPayment.getUser());
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(savedPayment.getAmount()));
        assertEquals(PaymentStatus.PENDING, savedPayment.getPaymentStatus());
        assertEquals(PaymentMethod.UPI, savedPayment.getPaymentMethod());
        assertNull(savedPayment.getPaidAt());
        assertTrue(savedPayment.getPaymentNumber().startsWith("PAY_"));
    }

    @Test
    void failedPayment_PendingPaymentForPlacedOrder_ShouldMarkPaymentFailedAndKeepOrderPlaced() {
        User customer = User.builder()
                .userId(12L)
                .build();

        Order order = Order.builder()
                .orderId(12L)
                .orderNumber("ORD-PAY-012")
                .orderStatus(OrderStatus.PLACED)
                .user(customer)
                .build();

        Payment payment = Payment.builder()
                .paymentId(12L)
                .order(order)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        PaymentResponseDto expectedResponse = PaymentResponseDto.builder()
                .build();

        when(paymentMapper.getPayment(12L)).thenReturn(payment);
        when(orderRepository.findWithLockByOrderId(12L)).thenReturn(Optional.of(order));
        when(paymentMapper.getPaymentWithLock(12L)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.mapPaymentToResponse(payment)).thenReturn(expectedResponse);

        PaymentResponseDto actualResponse =
                paymentServiceImpl.failedPayment(12L);
        assertEquals(expectedResponse, actualResponse);
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
        assertEquals(OrderStatus.PLACED, order.getOrderStatus());
        assertNull(payment.getPaidAt());
        verify(paymentRepository).save(payment);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void failedPayment_AlreadyFailedPayment_ShouldThrowPaymentStatusException() {
        Order order = Order.builder()
                .orderId(13L)
                .orderNumber("ORD-PAY-013")
                .orderStatus(OrderStatus.PLACED)
                .build();

        Payment payment = Payment.builder()
                .paymentId(13L)
                .order(order)
                .paymentStatus(PaymentStatus.FAILED)
                .build();

        when(paymentMapper.getPayment(13L)).thenReturn(payment);
        when(orderRepository.findWithLockByOrderId(13L)).thenReturn(Optional.of(order));
        when(paymentMapper.getPaymentWithLock(13L)).thenReturn(payment);
        assertThrows(PaymentStatusException.class,
                () -> paymentServiceImpl.failedPayment(13L));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentMapper, never()).mapPaymentToResponse(any(Payment.class));
    }

    @Test
    void failedPayment_ConfirmedOrder_ShouldThrowPaymentStatusException() {
        Order order = Order.builder()
                .orderId(14L)
                .orderNumber("ORD-PAY-014")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        Payment payment = Payment.builder()
                .paymentId(14L)
                .order(order)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(paymentMapper.getPayment(14L)).thenReturn(payment);
        when(orderRepository.findWithLockByOrderId(14L)).thenReturn(Optional.of(order));
        when(paymentMapper.getPaymentWithLock(14L)).thenReturn(payment);
        assertThrows(PaymentStatusException.class,
                () -> paymentServiceImpl.failedPayment(14L));
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentMapper, never()).mapPaymentToResponse(any(Payment.class));
    }

    @Test
    void getAllPayments_PaymentsExist_ShouldReturnMappedPayments() {
        User customer = User.builder()
                .userId(15L)
                .build();

        Order order = Order.builder()
                .orderId(15L)
                .user(customer)
                .build();

        Payment firstPayment = Payment.builder()
                .paymentId(15L)
                .order(order)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Payment secondPayment = Payment.builder()
                .paymentId(16L)
                .order(order)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        PaymentResponseDto firstResponse = PaymentResponseDto.builder()
                .build();
        PaymentResponseDto secondResponse = PaymentResponseDto.builder()
                .build();

        when(paymentRepository.findAll()).thenReturn(List.of(firstPayment, secondPayment));
        when(paymentMapper.mapPaymentToResponse(firstPayment)).thenReturn(firstResponse);
        when(paymentMapper.mapPaymentToResponse(secondPayment)).thenReturn(secondResponse);
        List<PaymentResponseDto> actualResponses =
                paymentServiceImpl.getAllPayments();

        assertEquals(2, actualResponses.size());
        assertSame(firstResponse, actualResponses.get(0));
        assertSame(secondResponse, actualResponses.get(1));
        verify(paymentRepository).findAll();
        verify(paymentMapper).mapPaymentToResponse(firstPayment);
        verify(paymentMapper).mapPaymentToResponse(secondPayment);
    }

    @Test
    void getAllPayments_NoPaymentsExist_ShouldReturnEmptyList() {
        when(paymentRepository.findAll()).thenReturn(List.of());
        List<PaymentResponseDto> actualResponses =
                paymentServiceImpl.getAllPayments();
        assertTrue(actualResponses.isEmpty());
        verify(paymentRepository).findAll();
        verifyNoInteractions(paymentMapper);
    }

    @Test
    void getUserAllPayments_UserHasPayments_ShouldReturnMappedPayments() {
        User customer = User.builder()
                .userId(17L)
                .build();

        Order order = Order.builder()
                .orderId(17L)
                .user(customer)
                .build();

        Payment payment = Payment.builder()
                .paymentId(17L)
                .order(order)
                .user(customer)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        PaymentResponseDto expectedResponse = PaymentResponseDto.builder()
                .build();
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(paymentRepository.findByUser(customer)).thenReturn(List.of(payment));
        when(paymentMapper.mapPaymentToResponse(payment)).thenReturn(expectedResponse);
        List<PaymentResponseDto> actualResponses =
                paymentServiceImpl.getUserAllPayments();
        assertEquals(1, actualResponses.size());
        assertSame(expectedResponse, actualResponses.getFirst());
        verify(paymentRepository).findByUser(customer);
        verify(paymentMapper).mapPaymentToResponse(payment);
    }

    @Test
    void getUserAllPayments_UserHasNoPayments_ShouldReturnEmptyList() {
        User customer = User.builder()
                .userId(18L)
                .build();

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(paymentRepository.findByUser(customer)).thenReturn(List.of());
        List<PaymentResponseDto> actualResponses =
                paymentServiceImpl.getUserAllPayments();
        assertTrue(actualResponses.isEmpty());
        verify(paymentRepository).findByUser(customer);
        verifyNoInteractions(paymentMapper);
    }
}
