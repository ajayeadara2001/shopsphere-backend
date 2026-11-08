package com.shopsphere.controllerTest;

import com.shopsphere.dto.CategoryResponseDto;
import com.shopsphere.dto.ProductResponseDto;
import com.shopsphere.service.CategoryService;
import com.shopsphere.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getActiveProducts_WithoutAuthentication_ShouldReturnPublicCatalog() throws Exception {
        ProductResponseDto product = ProductResponseDto.builder()
                .productId(1L)
                .productName("Public Product")
                .price(new BigDecimal("500.00"))
                .stockQuantity(10)
                .active(true)
                .categoryId(1L)
                .categoryName("Electronics")
                .build();

        when(productService.getAllActiveProducts())
                .thenReturn(List.of(product));

        mockMvc.perform(get("/api/v1/products/active/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].productName")
                        .value("Public Product"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(productService).getAllActiveProducts();
    }

    @Test
    void getActiveCategories_WithoutAuthentication_ShouldReturnPublicCatalog() throws Exception {
        CategoryResponseDto category = CategoryResponseDto.builder()
                .categoryId(1L)
                .categoryName("Electronics")
                .active(true)
                .build();

        when(categoryService.getAllActiveCategory())
                .thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/categories/active/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].categoryName")
                        .value("Electronics"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(categoryService).getAllActiveCategory();
    }
}