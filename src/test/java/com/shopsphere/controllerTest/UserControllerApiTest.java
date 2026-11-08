package com.shopsphere.controllerTest;

import com.shopsphere.constant.Role;
import com.shopsphere.dto.UserResponseDto;
import com.shopsphere.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void registerUser_ValidRequest_ShouldReturnCreatedUser() throws Exception {
        UserResponseDto response = UserResponseDto.builder()
                .userId(1L)
                .userName("Test Customer")
                .email("customer@shopsphere.com")
                .phoneNo("9876543210")
                .role(Role.CUSTOMER)
                .build();

        when(userService.registerUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "Test Customer",
                                  "email": "customer@shopsphere.com",
                                  "phoneNo": "9876543210",
                                  "password": "Valid@Password1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email")
                        .value("customer@shopsphere.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        verify(userService).registerUser(any());
    }

    @Test
    void registerUser_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "",
                                  "email": "invalid-email",
                                  "phoneNo": "123",
                                  "password": "weak"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(userService);
    }

    @Test
    void updateUser_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "Updated Customer",
                                  "email": "customer@shopsphere.com",
                                  "phoneNo": "9876543210",
                                  "password": "Valid@Password1"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void updateUser_AsCustomer_ShouldReturnUpdatedUser() throws Exception {
        UserResponseDto response = UserResponseDto.builder()
                .userId(1L)
                .userName("Updated Customer")
                .email("customer@shopsphere.com")
                .phoneNo("9876543210")
                .role(Role.CUSTOMER)
                .build();

        when(userService.updateUser(eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "Updated Customer",
                                  "email": "customer@shopsphere.com",
                                  "phoneNo": "9876543210",
                                  "password": "Valid@Password1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.userName")
                        .value("Updated Customer"));

        verify(userService).updateUser(eq(1L), any());
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void updateUser_AsAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "Updated Customer",
                                  "email": "customer@shopsphere.com",
                                  "phoneNo": "9876543210",
                                  "password": "Valid@Password1"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }
}