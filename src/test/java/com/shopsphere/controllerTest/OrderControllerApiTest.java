package com.shopsphere.controllerTest;

import com.shopsphere.constant.OrderStatus;
import com.shopsphere.dto.OrderResponseDto;
import com.shopsphere.dto.OrderStatusUpdateRequestDto;
import com.shopsphere.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void placeOrder_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/orders"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    @Test
    @WithMockUser(
            username = "customer@shopsphere.com",
            roles = "CUSTOMER"
    )
    void placeOrder_AsCustomer_ShouldReturnCreatedOrder() throws Exception {
        OrderResponseDto response = OrderResponseDto.builder()
                .orderId(1L)
                .orderNumber("ORD-API-001")
                .orderStatus(OrderStatus.PLACED)
                .build();

        when(orderService.placeOrder("SAVE10"))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .param("couponCode", "SAVE10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderNumber")
                        .value("ORD-API-001"))
                .andExpect(jsonPath("$.orderStatus")
                        .value("PLACED"));
        verify(orderService).placeOrder("SAVE10");
    }

    @Test
    @WithMockUser(
            username = "admin@shopsphere.com",
            roles = "ADMIN"
    )
    void placeOrder_AsAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/orders"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orderService);
    }


    @Test
    void updateOrderStatus_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/admin/orders/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"orderStatus":"CONFIRMED"}
                            """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    @Test
    @WithMockUser(
            username = "customer@shopsphere.com",
            roles = "CUSTOMER"
    )
    void updateOrderStatus_AsCustomer_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/admin/orders/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"orderStatus":"CONFIRMED"}
                            """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    @Test
    @WithMockUser(
            username = "admin@shopsphere.com",
            roles = "ADMIN"
    )
    void updateOrderStatus_AsAdmin_ShouldReturnUpdatedOrder() throws Exception {
        OrderResponseDto response = OrderResponseDto.builder()
                .orderId(10L)
                .orderNumber("ORD-API-010")
                .orderStatus(OrderStatus.CONFIRMED)
                .build();

        when(orderService.updateOrderStatus(
                eq(10L),
                any(OrderStatusUpdateRequestDto.class)
        )).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/orders/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"orderStatus":"CONFIRMED"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.orderNumber")
                        .value("ORD-API-010"))
                .andExpect(jsonPath("$.orderStatus")
                        .value("CONFIRMED"));

        verify(orderService).updateOrderStatus(
                eq(10L),
                any(OrderStatusUpdateRequestDto.class)
        );
    }
}