package com.shopsphere.controllerTest;

import com.shopsphere.constant.RefundStatus;
import com.shopsphere.dto.RefundResponseDto;
import com.shopsphere.service.RefundService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class RefundControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RefundService refundService;

    @Test
    void applyRefund_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": 1,
                                  "refundAmount": 100.00,
                                  "refundReason": "Damaged product"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(refundService);
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void applyRefund_AsAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": 1,
                                  "refundAmount": 100.00,
                                  "refundReason": "Damaged product"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(refundService);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void applyRefund_ValidRequest_ShouldReturnCreatedRefund() throws Exception {
        RefundResponseDto response = RefundResponseDto.builder()
                .refundId(1L)
                .paymentId(1L)
                .paymentNumber("PAY-API-001")
                .refundAmount(new BigDecimal("100.00"))
                .refundReason("Damaged product")
                .refundStatus(RefundStatus.PENDING)
                .build();

        when(refundService.applyRefund(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": 1,
                                  "refundAmount": 100.00,
                                  "refundReason": "Damaged product"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refundId").value(1))
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.paymentNumber")
                        .value("PAY-API-001"))
                .andExpect(jsonPath("$.refundAmount").value(100.00))
                .andExpect(jsonPath("$.refundStatus").value("PENDING"));

        verify(refundService).applyRefund(any());
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void applyRefund_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": 1,
                                  "refundAmount": 0,
                                  "refundReason": "bad"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(refundService);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void getRefundById_AsCustomer_ShouldReturnRefund() throws Exception {
        RefundResponseDto response = RefundResponseDto.builder()
                .refundId(1L)
                .paymentId(1L)
                .paymentNumber("PAY-API-001")
                .refundAmount(new BigDecimal("100.00"))
                .refundReason("Damaged product")
                .refundStatus(RefundStatus.PENDING)
                .build();

        when(refundService.getRefundById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/refund/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value(1))
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.refundStatus").value("PENDING"));

        verify(refundService).getRefundById(1L);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void getRefundByPaymentId_AsCustomer_ShouldReturnRefundHistory() throws Exception {
        RefundResponseDto firstRefund = RefundResponseDto.builder()
                .refundId(1L)
                .paymentId(1L)
                .refundAmount(new BigDecimal("100.00"))
                .refundStatus(RefundStatus.COMPLETED)
                .build();

        RefundResponseDto secondRefund = RefundResponseDto.builder()
                .refundId(2L)
                .paymentId(1L)
                .refundAmount(new BigDecimal("50.00"))
                .refundStatus(RefundStatus.PENDING)
                .build();

        when(refundService.getRefundByPaymentId(1L))
                .thenReturn(List.of(firstRefund, secondRefund));

        mockMvc.perform(get("/api/v1/refund/payment/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].refundStatus")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$[1].refundStatus")
                        .value("PENDING"));

        verify(refundService).getRefundByPaymentId(1L);
    }
}