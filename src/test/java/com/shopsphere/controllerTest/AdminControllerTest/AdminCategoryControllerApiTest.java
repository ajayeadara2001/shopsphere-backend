package com.shopsphere.controllerTest.AdminControllerTest;

import com.shopsphere.dto.CategoryResponseDto;
import com.shopsphere.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminCategoryControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void createCategory_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCategoryRequest()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void createCategory_AsCustomer_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCategoryRequest()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void createCategory_AsAdmin_ShouldReturnCreatedCategory() throws Exception {
        CategoryResponseDto response = CategoryResponseDto.builder()
                .categoryId(1L)
                .categoryName("Electronics")
                .description("Electronic products and accessories")
                .active(true)
                .build();

        when(categoryService.createCategory(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCategoryRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName")
                        .value("Electronics"))
                .andExpect(jsonPath("$.active").value(true));

        verify(categoryService).createCategory(any());
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void createCategory_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryName": "",
                                  "description": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void getAllCategories_AsAdmin_ShouldReturnActiveAndInactiveCategories() throws Exception {
        CategoryResponseDto activeCategory = CategoryResponseDto.builder()
                .categoryId(1L)
                .categoryName("Electronics")
                .active(true)
                .build();

        CategoryResponseDto inactiveCategory = CategoryResponseDto.builder()
                .categoryId(2L)
                .categoryName("Retired Category")
                .active(false)
                .build();

        when(categoryService.getAllCategory())
                .thenReturn(List.of(activeCategory, inactiveCategory));

        mockMvc.perform(get("/api/v1/admin/categories/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].active").value(false));

        verify(categoryService).getAllCategory();
    }

    private String validCategoryRequest() {
        return """
                {
                  "categoryName": "Electronics",
                  "description": "Electronic products and accessories"
                }
                """;
    }
}