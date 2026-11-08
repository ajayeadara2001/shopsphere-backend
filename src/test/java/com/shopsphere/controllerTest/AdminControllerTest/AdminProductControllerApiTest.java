package com.shopsphere.controllerTest;

import com.shopsphere.dto.ProductResponseDto;
import com.shopsphere.service.ProductService;
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
class AdminProductControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createProduct_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductRequest()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void createProduct_AsCustomer_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductRequest()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void createProduct_AsAdmin_ShouldReturnCreatedProduct() throws Exception {
        ProductResponseDto response = ProductResponseDto.builder()
                .productId(1L)
                .productName("Admin Product")
                .description("A product created through the admin API")
                .price(new BigDecimal("500.00"))
                .stockQuantity(10)
                .brand("ShopSphere")
                .imageUrl("https://example.com/product.jpg")
                .categoryId(1L)
                .active(true)
                .build();
        when(productService.createProduct(any())).thenReturn(response);
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName")
                        .value("Admin Product"))
                .andExpect(jsonPath("$.active").value(true));
        verify(productService).createProduct(any());
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void createProduct_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productName": "",
                                  "price": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void getAllProducts_AsAdmin_ShouldReturnActiveAndInactiveProducts() throws Exception {
        ProductResponseDto activeProduct = ProductResponseDto.builder()
                .productId(1L)
                .productName("Active Product")
                .active(true)
                .build();

        ProductResponseDto inactiveProduct = ProductResponseDto.builder()
                .productId(2L)
                .productName("Inactive Product")
                .active(false)
                .build();

        when(productService.getAllProducts())
                .thenReturn(List.of(activeProduct, inactiveProduct));
        mockMvc.perform(get("/api/v1/admin/products/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].active").value(false));
        verify(productService).getAllProducts();
    }

    private String validProductRequest() {
        return """
                {
                  "productName": "Admin Product",
                  "description": "A product created through the admin API",
                  "price": 500.00,
                  "stockQuantity": 10,
                  "brand": "ShopSphere",
                  "imageUrl": "https://example.com/product.jpg",
                  "categoryId": 1
                }
                """;
    }
}