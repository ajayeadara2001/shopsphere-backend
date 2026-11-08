package com.shopsphere.service.serviceImpl;

import com.shopsphere.constant.PaymentStatus;
import com.shopsphere.constant.RefundStatus;
import com.shopsphere.constant.Role;
import com.shopsphere.dto.RefundRequestDto;
import com.shopsphere.dto.RefundResponseDto;
import com.shopsphere.dto.RefundStatusRequestDto;
import com.shopsphere.entity.Payment;
import com.shopsphere.entity.Refund;
import com.shopsphere.entity.User;
import com.shopsphere.exception.AccessDeniedException;
import com.shopsphere.exception.PaymentStatusException;
import com.shopsphere.exception.RefundException;
import com.shopsphere.mapper.PaymentMapper;
import com.shopsphere.mapper.RefundMapper;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.PaymentRepository;
import com.shopsphere.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefundServiceImplTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundMapper refundMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private RefundServiceImpl refundServiceImpl;

    @Test
    void applyRefund_OwnerRequestsValidRefund_ShouldCreatePendingRefund() {
        User customer = user(1L, Role.CUSTOMER);
        Payment payment = payment(1L, customer, PaymentStatus.SUCCESS, "500.00");
        RefundRequestDto request = new RefundRequestDto(
                1L,
                new BigDecimal("200.00"),
                "Product arrived damaged"
        );

        RefundResponseDto expectedResponse = RefundResponseDto.builder()
                .refundStatus(RefundStatus.PENDING)
                .build();

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(paymentMapper.getPaymentWithLock(1L)).thenReturn(payment);
        when(refundRepository.findByPaymentPaymentId(1L))
                .thenReturn(List.of());
        when(refundRepository.save(any(Refund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(refundMapper.mapRefundToResponse(any(Refund.class)))
                .thenReturn(expectedResponse);

        RefundResponseDto actualResponse =
                refundServiceImpl.applyRefund(request);

        assertSame(expectedResponse, actualResponse);

        ArgumentCaptor<Refund> refundCaptor =
                ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(refundCaptor.capture());

        Refund savedRefund = refundCaptor.getValue();
        assertSame(payment, savedRefund.getPayment());
        assertEquals(0, new BigDecimal("200.00")
                .compareTo(savedRefund.getRefundAmount()));
        assertEquals("Product arrived damaged", savedRefund.getRefundReason());
        assertEquals(RefundStatus.PENDING, savedRefund.getRefundStatus());
    }

    @Test
    void applyRefund_AnotherCustomerRequestsRefund_ShouldThrowAccessDeniedException() {
        User paymentOwner = user(2L, Role.CUSTOMER);
        User otherCustomer = user(3L, Role.CUSTOMER);
        Payment payment = payment(2L, paymentOwner, PaymentStatus.SUCCESS, "500.00");

        when(userMapper.getCurrentUser()).thenReturn(otherCustomer);
        when(paymentMapper.getPaymentWithLock(2L)).thenReturn(payment);

        assertThrows(AccessDeniedException.class,
                () -> refundServiceImpl.applyRefund(
                        new RefundRequestDto(
                                2L,
                                new BigDecimal("100.00"),
                                "Wrong item received"
                        )));

        verifyNoInteractions(refundRepository);
    }

    @Test
    void applyRefund_PaymentIsNotSuccessful_ShouldThrowPaymentStatusException() {
        User customer = user(4L, Role.CUSTOMER);
        Payment payment = payment(4L, customer, PaymentStatus.PENDING, "500.00");

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(paymentMapper.getPaymentWithLock(4L)).thenReturn(payment);

        assertThrows(PaymentStatusException.class,
                () -> refundServiceImpl.applyRefund(
                        new RefundRequestDto(
                                4L,
                                new BigDecimal("100.00"),
                                "Changed my mind"
                        )));

        verifyNoInteractions(refundRepository);
    }

    @Test
    void applyRefund_AmountExceedsRemainingRefundableAmount_ShouldThrowRefundException() {
        User customer = user(5L, Role.CUSTOMER);
        Payment payment = payment(5L, customer, PaymentStatus.SUCCESS, "500.00");
        Refund committedRefund = refund(
                5L, payment, "400.00", RefundStatus.COMPLETED);

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(paymentMapper.getPaymentWithLock(5L)).thenReturn(payment);
        when(refundRepository.findByPaymentPaymentId(5L))
                .thenReturn(List.of(committedRefund));

        assertThrows(RefundException.class,
                () -> refundServiceImpl.applyRefund(
                        new RefundRequestDto(
                                5L,
                                new BigDecimal("150.00"),
                                "Request exceeds balance"
                        )));

        verify(refundRepository, never()).save(any(Refund.class));
    }

    @Test
    void applyRefund_PendingRefundConsumesRemainingAmount_ShouldThrowRefundException() {
        User customer = user(6L, Role.CUSTOMER);
        Payment payment = payment(6L, customer, PaymentStatus.SUCCESS, "500.00");
        Refund pendingRefund = refund(
                6L, payment, "500.00", RefundStatus.PENDING);

        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(paymentMapper.getPaymentWithLock(6L)).thenReturn(payment);
        when(refundRepository.findByPaymentPaymentId(6L))
                .thenReturn(List.of(pendingRefund));

        assertThrows(RefundException.class,
                () -> refundServiceImpl.applyRefund(
                        new RefundRequestDto(
                                6L,
                                new BigDecimal("1.00"),
                                "Duplicate refund request"
                        )));

        verify(refundRepository, never()).save(any(Refund.class));
    }

    @Test
    void getRefundById_OwnerRequestsRefund_ShouldReturnMappedRefund() {
        User customer = user(7L, Role.CUSTOMER);
        Payment payment = payment(7L, customer, PaymentStatus.SUCCESS, "500.00");
        Refund refund = refund(7L, payment, "100.00", RefundStatus.PENDING);
        RefundResponseDto expectedResponse = RefundResponseDto.builder()
                .refundId(7L)
                .refundStatus(RefundStatus.PENDING)
                .build();

        when(refundMapper.getRefundById(7L)).thenReturn(refund);
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(refundMapper.mapRefundToResponse(refund))
                .thenReturn(expectedResponse);

        RefundResponseDto actualResponse =
                refundServiceImpl.getRefundById(7L);

        assertSame(expectedResponse, actualResponse);
        verify(refundMapper).getRefundById(7L);
        verify(refundMapper).mapRefundToResponse(refund);
    }

    @Test
    void getRefundById_AnotherCustomerRequestsRefund_ShouldThrowAccessDeniedException() {
        User paymentOwner = user(8L, Role.CUSTOMER);
        User otherCustomer = user(9L, Role.CUSTOMER);
        Payment payment = payment(8L, paymentOwner, PaymentStatus.SUCCESS, "500.00");
        Refund refund = refund(8L, payment, "100.00", RefundStatus.PENDING);

        when(refundMapper.getRefundById(8L)).thenReturn(refund);
        when(userMapper.getCurrentUser()).thenReturn(otherCustomer);

        assertThrows(AccessDeniedException.class,
                () -> refundServiceImpl.getRefundById(8L));

        verify(refundMapper, never()).mapRefundToResponse(any(Refund.class));
    }

    @Test
    void getRefundById_AdminRequestsAnotherCustomersRefund_ShouldReturnMappedRefund() {
        User paymentOwner = user(10L, Role.CUSTOMER);
        User admin = user(11L, Role.ADMIN);
        Payment payment = payment(10L, paymentOwner, PaymentStatus.SUCCESS, "500.00");
        Refund refund = refund(10L, payment, "100.00", RefundStatus.PENDING);
        RefundResponseDto expectedResponse = RefundResponseDto.builder().build();

        when(refundMapper.getRefundById(10L)).thenReturn(refund);
        when(userMapper.getCurrentUser()).thenReturn(admin);
        when(refundMapper.mapRefundToResponse(refund))
                .thenReturn(expectedResponse);

        RefundResponseDto actualResponse =
                refundServiceImpl.getRefundById(10L);

        assertSame(expectedResponse, actualResponse);
    }

    @Test
    void getRefundByPaymentId_OwnerRequestsRefunds_ShouldReturnMappedRefunds() {
        User customer = user(12L, Role.CUSTOMER);
        Payment payment = payment(12L, customer, PaymentStatus.SUCCESS, "500.00");
        Refund firstRefund = refund(12L, payment, "100.00", RefundStatus.COMPLETED);
        Refund secondRefund = refund(13L, payment, "50.00", RefundStatus.PENDING);

        RefundResponseDto firstResponse = RefundResponseDto.builder().build();
        RefundResponseDto secondResponse = RefundResponseDto.builder().build();

        when(paymentMapper.getPayment(12L)).thenReturn(payment);
        when(userMapper.getCurrentUser()).thenReturn(customer);
        when(refundRepository.findByPaymentPaymentId(12L))
                .thenReturn(List.of(firstRefund, secondRefund));
        when(refundMapper.mapRefundToResponse(firstRefund))
                .thenReturn(firstResponse);
        when(refundMapper.mapRefundToResponse(secondRefund))
                .thenReturn(secondResponse);

        List<RefundResponseDto> actualResponses =
                refundServiceImpl.getRefundByPaymentId(12L);

        assertEquals(2, actualResponses.size());
        assertSame(firstResponse, actualResponses.get(0));
        assertSame(secondResponse, actualResponses.get(1));
    }

    @Test
    void getRefundByPaymentId_AnotherCustomerRequestsRefunds_ShouldThrowAccessDeniedException() {
        User paymentOwner = user(13L, Role.CUSTOMER);
        User otherCustomer = user(14L, Role.CUSTOMER);
        Payment payment = payment(13L, paymentOwner, PaymentStatus.SUCCESS, "500.00");

        when(paymentMapper.getPayment(13L)).thenReturn(payment);
        when(userMapper.getCurrentUser()).thenReturn(otherCustomer);

        assertThrows(AccessDeniedException.class,
                () -> refundServiceImpl.getRefundByPaymentId(13L));

        verifyNoInteractions(refundRepository);
    }

    @Test
    void getAllRefunds_RefundsExist_ShouldReturnMappedRefunds() {
        User customer = user(15L, Role.CUSTOMER);
        Payment payment = payment(15L, customer, PaymentStatus.SUCCESS, "500.00");
        Refund firstRefund = refund(15L, payment, "100.00", RefundStatus.PENDING);
        Refund secondRefund = refund(16L, payment, "50.00", RefundStatus.FAILED);

        RefundResponseDto firstResponse = RefundResponseDto.builder().build();
        RefundResponseDto secondResponse = RefundResponseDto.builder().build();

        when(refundRepository.findAll())
                .thenReturn(List.of(firstRefund, secondRefund));
        when(refundMapper.mapRefundToResponse(firstRefund))
                .thenReturn(firstResponse);
        when(refundMapper.mapRefundToResponse(secondRefund))
                .thenReturn(secondResponse);

        List<RefundResponseDto> actualResponses =
                refundServiceImpl.getAllRefunds();

        assertEquals(2, actualResponses.size());
        assertSame(firstResponse, actualResponses.get(0));
        assertSame(secondResponse, actualResponses.get(1));
    }

    @Test
    void getAllRefunds_NoRefundsExist_ShouldReturnEmptyList() {
        when(refundRepository.findAll()).thenReturn(List.of());

        List<RefundResponseDto> actualResponses =
                refundServiceImpl.getAllRefunds();

        assertTrue(actualResponses.isEmpty());
        verifyNoInteractions(refundMapper);
    }

    @Test
    void updateRefund_CompletePartialRefund_ShouldMarkRefundCompleted() {
        User customer = user(17L, Role.CUSTOMER);
        Payment payment = payment(17L, customer, PaymentStatus.SUCCESS, "500.00");
        Refund refund = refund(17L, payment, "100.00", RefundStatus.PENDING);
        RefundResponseDto expectedResponse = RefundResponseDto.builder()
                .refundStatus(RefundStatus.COMPLETED)
                .build();

        when(refundMapper.getRefundById(17L)).thenReturn(refund);
        when(paymentMapper.getPaymentWithLock(17L)).thenReturn(payment);
        when(refundRepository.findByPaymentPaymentId(17L))
                .thenReturn(List.of(refund));
        when(refundRepository.save(refund)).thenReturn(refund);
        when(refundMapper.mapRefundToResponse(refund))
                .thenReturn(expectedResponse);

        RefundResponseDto actualResponse = refundServiceImpl.updateRefund(
                17L,
                new RefundStatusRequestDto(RefundStatus.COMPLETED)
        );

        assertSame(expectedResponse, actualResponse);
        assertEquals(RefundStatus.COMPLETED, refund.getRefundStatus());
        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());

        verify(refundRepository).save(refund);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void updateRefund_CompleteFinalRefund_ShouldMarkPaymentRefunded() {
        User customer = user(18L, Role.CUSTOMER);
        Payment payment = payment(18L, customer, PaymentStatus.SUCCESS, "500.00");
        Refund refund = refund(18L, payment, "500.00", RefundStatus.PENDING);
        RefundResponseDto expectedResponse = RefundResponseDto.builder()
                .refundStatus(RefundStatus.COMPLETED)
                .build();

        when(refundMapper.getRefundById(18L)).thenReturn(refund);
        when(paymentMapper.getPaymentWithLock(18L)).thenReturn(payment);
        when(refundRepository.findByPaymentPaymentId(18L))
                .thenReturn(List.of(refund));
        when(refundRepository.save(refund)).thenReturn(refund);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(refundMapper.mapRefundToResponse(refund))
                .thenReturn(expectedResponse);

        RefundResponseDto actualResponse = refundServiceImpl.updateRefund(
                18L,
                new RefundStatusRequestDto(RefundStatus.COMPLETED)
        );

        assertSame(expectedResponse, actualResponse);
        assertEquals(RefundStatus.COMPLETED, refund.getRefundStatus());
        assertEquals(PaymentStatus.REFUNDED, payment.getPaymentStatus());

        verify(refundRepository).save(refund);
        verify(paymentRepository).save(payment);
    }

    @Test
    void updateRefund_FailPendingRefund_ShouldMarkRefundFailed() {
        User customer = user(19L, Role.CUSTOMER);
        Payment payment = payment(19L, customer, PaymentStatus.SUCCESS, "500.00");
        Refund refund = refund(19L, payment, "100.00", RefundStatus.PENDING);
        RefundResponseDto expectedResponse = RefundResponseDto.builder()
                .refundStatus(RefundStatus.FAILED)
                .build();

        when(refundMapper.getRefundById(19L)).thenReturn(refund);
        when(paymentMapper.getPaymentWithLock(19L)).thenReturn(payment);
        when(refundRepository.save(refund)).thenReturn(refund);
        when(refundMapper.mapRefundToResponse(refund))
                .thenReturn(expectedResponse);

        RefundResponseDto actualResponse = refundServiceImpl.updateRefund(
                19L,
                new RefundStatusRequestDto(RefundStatus.FAILED)
        );

        assertSame(expectedResponse, actualResponse);
        assertEquals(RefundStatus.FAILED, refund.getRefundStatus());
        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());

        verify(refundRepository).save(refund);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void updateRefund_RefundIsNotPending_ShouldThrowRefundException() {
        User customer = user(20L, Role.CUSTOMER);
        Payment payment = payment(20L, customer, PaymentStatus.SUCCESS, "500.00");
        Refund refund = refund(20L, payment, "100.00", RefundStatus.COMPLETED);

        when(refundMapper.getRefundById(20L)).thenReturn(refund);
        when(paymentMapper.getPaymentWithLock(20L)).thenReturn(payment);

        assertThrows(RefundException.class,
                () -> refundServiceImpl.updateRefund(
                        20L,
                        new RefundStatusRequestDto(RefundStatus.FAILED)
                ));

        verify(refundRepository, never()).save(any(Refund.class));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void updateRefund_CompleteRefundForUnsuccessfulPayment_ShouldThrowPaymentStatusException() {
        User customer = user(21L, Role.CUSTOMER);
        Payment payment = payment(21L, customer, PaymentStatus.FAILED, "500.00");
        Refund refund = refund(21L, payment, "100.00", RefundStatus.PENDING);

        when(refundMapper.getRefundById(21L)).thenReturn(refund);
        when(paymentMapper.getPaymentWithLock(21L)).thenReturn(payment);

        assertThrows(PaymentStatusException.class,
                () -> refundServiceImpl.updateRefund(
                        21L,
                        new RefundStatusRequestDto(RefundStatus.COMPLETED)
                ));

        assertEquals(RefundStatus.PENDING, refund.getRefundStatus());
        verify(refundRepository, never()).save(any(Refund.class));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void updateRefund_RequestPendingStatus_ShouldThrowRefundException() {
        User customer = user(22L, Role.CUSTOMER);
        Payment payment = payment(22L, customer, PaymentStatus.SUCCESS, "500.00");
        Refund refund = refund(22L, payment, "100.00", RefundStatus.PENDING);

        when(refundMapper.getRefundById(22L)).thenReturn(refund);
        when(paymentMapper.getPaymentWithLock(22L)).thenReturn(payment);

        assertThrows(RefundException.class,
                () -> refundServiceImpl.updateRefund(
                        22L,
                        new RefundStatusRequestDto(RefundStatus.PENDING)
                ));

        assertEquals(RefundStatus.PENDING, refund.getRefundStatus());
        verify(refundRepository, never()).save(any(Refund.class));
        verifyNoInteractions(paymentRepository);
    }

    private User user(Long userId, Role role) {
        return User.builder()
                .userId(userId)
                .role(role)
                .build();
    }
    private Payment payment(
            Long paymentId,
            User user,
            PaymentStatus status,
            String amount
    ) {
        return Payment.builder()
                .paymentId(paymentId)
                .paymentNumber("PAY-" + paymentId)
                .user(user)
                .amount(new BigDecimal(amount))
                .paymentStatus(status)
                .build();
    }
    private Refund refund(
            Long refundId,
            Payment payment,
            String amount,
            RefundStatus status
    ) {
        return Refund.builder()
                .refundId(refundId)
                .payment(payment)
                .refundAmount(new BigDecimal(amount))
                .refundStatus(status)
                .build();
    }
}