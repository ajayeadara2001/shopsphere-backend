package com.shopsphere.controllerTest.AdminControllerTest;

import com.shopsphere.constant.PaymentMethod;
import com.shopsphere.constant.PaymentStatus;
import com.shopsphere.dto.PaymentResponseDto;
import com.shopsphere.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminPaymentControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void successPayment_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/admin/payments/1/success"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void successPayment_AsCustomer_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/admin/payments/1/success"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(paymentService);
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void successPayment_AsAdmin_ShouldReturnSuccessfulPayment() throws Exception {
        PaymentResponseDto response = PaymentResponseDto.builder()
                .paymentId(1L)
                .paymentNumber("PAY-ADMIN-001")
                .orderId(1L)
                .amount(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentMethod(PaymentMethod.UPI)
                .build();

        when(paymentService.successPayment(1L)).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/payments/1/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        verify(paymentService).successPayment(1L);
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void failedPayment_AsAdmin_ShouldReturnFailedPayment() throws Exception {
        PaymentResponseDto response = PaymentResponseDto.builder()
                .paymentId(2L)
                .paymentNumber("PAY-ADMIN-002")
                .orderId(2L)
                .amount(new BigDecimal("250.00"))
                .paymentStatus(PaymentStatus.FAILED)
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(paymentService.failedPayment(2L)).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/payments/2/failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(2))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));

        verify(paymentService).failedPayment(2L);
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void getAllPayments_AsAdmin_ShouldReturnPaymentHistory() throws Exception {
        PaymentResponseDto firstPayment = PaymentResponseDto.builder()
                .paymentId(1L)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        PaymentResponseDto secondPayment = PaymentResponseDto.builder()
                .paymentId(2L)
                .paymentStatus(PaymentStatus.FAILED)
                .build();

        when(paymentService.getAllPayments())
                .thenReturn(List.of(firstPayment, secondPayment));

        mockMvc.perform(get("/api/v1/admin/payments/payment-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$[1].paymentStatus").value("FAILED"));

        verify(paymentService).getAllPayments();
    }
}