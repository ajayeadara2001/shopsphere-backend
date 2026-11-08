package com.shopsphere.controllerTest;

import com.shopsphere.dto.CartItemsResponseDto;
import com.shopsphere.dto.CartResponseDto;
import com.shopsphere.service.CartService;
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
class CartControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void addCartItem_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 1, "quantity": 2}
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void addCartItem_ValidRequest_ShouldReturnCreatedCart() throws Exception {
        CartResponseDto response = CartResponseDto.builder()
                .cartId(1L)
                .totalAmount(new BigDecimal("500.00"))
                .cartItemsList(List.of(
                        CartItemsResponseDto.builder()
                                .productId(1L)
                                .productName("Test Product")
                                .productPrice(new BigDecimal("250.00"))
                                .quantity(2)
                                .subTotal(new BigDecimal("500.00"))
                                .build()
                ))
                .build();

        when(cartService.createCart(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 1, "quantity": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.totalAmount").value(500.00))
                .andExpect(jsonPath("$.cartItemsList[0].productId").value(1))
                .andExpect(jsonPath("$.cartItemsList[0].quantity").value(2));

        verify(cartService).createCart(any());
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void getCart_ShouldReturnCurrentCustomersCart() throws Exception {
        CartResponseDto response = CartResponseDto.builder()
                .cartId(1L)
                .totalAmount(new BigDecimal("250.00"))
                .cartItemsList(List.of(
                        CartItemsResponseDto.builder()
                                .productId(1L)
                                .productName("Test Product")
                                .quantity(1)
                                .build()
                ))
                .build();

        when(cartService.getCart()).thenReturn(response);

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.cartItemsList.length()").value(1));

        verify(cartService).getCart();
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void updateCartItem_QuantityBelowMinimum_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/cart/items/1")
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(cartService);
    }
    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void getCart_AsAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void updateCartItem_ValidQuantity_ShouldReturnUpdatedCart() throws Exception {
        CartResponseDto response = CartResponseDto.builder()
                .cartId(1L)
                .totalAmount(new BigDecimal("750.00"))
                .cartItemsList(List.of(
                        CartItemsResponseDto.builder()
                                .productId(1L)
                                .quantity(3)
                                .subTotal(new BigDecimal("750.00"))
                                .build()
                ))
                .build();

        when(cartService.updateCartItems(1L, 3))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/cart/items/1")
                        .param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(750.00))
                .andExpect(jsonPath("$.cartItemsList[0].quantity").value(3));

        verify(cartService).updateCartItems(1L, 3);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void clearCart_AsCustomer_ShouldReturnSuccessMessage() throws Exception {
        mockMvc.perform(delete("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cart cleared successfully"));

        verify(cartService).clearCart();
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void removeCartItem_AsCustomer_ShouldReturnSuccessMessage() throws Exception {
        mockMvc.perform(delete("/api/v1/cart/items/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Items removed from cart successfully"));

        verify(cartService).removeCartItem(1L);
    }
}