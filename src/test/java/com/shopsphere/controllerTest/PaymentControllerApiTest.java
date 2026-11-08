package com.shopsphere.controllerTest;

import com.shopsphere.constant.PaymentMethod;
import com.shopsphere.constant.PaymentStatus;
import com.shopsphere.dto.PaymentResponseDto;
import com.shopsphere.entity.Payment;
import com.shopsphere.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class PaymentControllerApiTest {
    @MockitoBean
    private PaymentService paymentService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createPayment_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/payments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod":"UPI"}
                        """))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(paymentService);
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void createPayment_AsAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/payments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod":"UPI"}
                        """))
                .andExpect(status().isForbidden());
        verifyNoInteractions(paymentService);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void createPayment_AsCustomer_ShouldReturnCreatedPayment() throws Exception {
        PaymentResponseDto response = PaymentResponseDto.builder()
                .paymentId(1L)
                .paymentNumber("PAY-API-001")
                .orderId(1L)
                .amount(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.UPI)
                .build();
        when(paymentService.createPayment(
                eq(1L),
                any())).thenReturn(response);
        mockMvc.perform(post("/api/v1/payments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"paymentMethod":"UPI"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.paymentNumber").value("PAY-API-001"))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("UPI"));
        verify(paymentService).createPayment(eq(1L),any());
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void createPayment_WithoutPaymentMethod_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/payments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(paymentService);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void getUserPayments_AsCustomer_ShouldReturnPaymentHistory() throws Exception {
        PaymentResponseDto firstPayment = PaymentResponseDto.builder()
                .paymentId(1L)
                .paymentNumber("PAY-API-001")
                .amount(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentMethod(PaymentMethod.UPI)
                .orderId(1L)
                .build();

        PaymentResponseDto secondPayment = PaymentResponseDto.builder()
                .paymentId(2L)
                .paymentNumber("PAY-API-002")
                .amount(new BigDecimal("250.00"))
                .paymentStatus(PaymentStatus.FAILED)
                .paymentMethod(PaymentMethod.CARD)
                .orderId(2L)
                .build();

        when(paymentService.getUserAllPayments())
                .thenReturn(List.of(firstPayment, secondPayment));

        mockMvc.perform(get("/api/v1/payments/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].paymentStatus")
                        .value("SUCCESS"))
                .andExpect(jsonPath("$[1].paymentStatus")
                        .value("FAILED"));

        verify(paymentService).getUserAllPayments();
    }
}
