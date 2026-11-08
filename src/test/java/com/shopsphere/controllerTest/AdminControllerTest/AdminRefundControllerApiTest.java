package com.shopsphere.controllerTest.AdminControllerTest;

import com.shopsphere.constant.RefundStatus;
import com.shopsphere.dto.RefundResponseDto;
import com.shopsphere.dto.RefundStatusRequestDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminRefundControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RefundService refundService;

    @Test
    void updateRefund_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/admin/refund/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refundStatus":"COMPLETED"}
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(refundService);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void updateRefund_AsCustomer_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/admin/refund/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refundStatus":"COMPLETED"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(refundService);
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void updateRefund_AsAdmin_ShouldReturnUpdatedRefund() throws Exception {
        RefundResponseDto response = RefundResponseDto.builder()
                .refundId(1L)
                .paymentId(1L)
                .paymentNumber("PAY-ADMIN-001")
                .refundAmount(new BigDecimal("500.00"))
                .refundReason("Approved refund")
                .refundStatus(RefundStatus.COMPLETED)
                .build();

        when(refundService.updateRefund(
                eq(1L),
                any(RefundStatusRequestDto.class)
        )).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/refund/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refundStatus":"COMPLETED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value(1))
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.refundStatus")
                        .value("COMPLETED"));

        verify(refundService).updateRefund(
                eq(1L),
                any(RefundStatusRequestDto.class)
        );
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void updateRefund_WithoutStatus_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/admin/refund/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(refundService);
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void getAllRefunds_AsAdmin_ShouldReturnRefundHistory() throws Exception {
        RefundResponseDto completedRefund = RefundResponseDto.builder()
                .refundId(1L)
                .refundStatus(RefundStatus.COMPLETED)
                .build();

        RefundResponseDto pendingRefund = RefundResponseDto.builder()
                .refundId(2L)
                .refundStatus(RefundStatus.PENDING)
                .build();

        when(refundService.getAllRefunds())
                .thenReturn(List.of(completedRefund, pendingRefund));

        mockMvc.perform(get("/api/v1/admin/refund/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].refundStatus")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$[1].refundStatus")
                        .value("PENDING"));

        verify(refundService).getAllRefunds();
    }
}