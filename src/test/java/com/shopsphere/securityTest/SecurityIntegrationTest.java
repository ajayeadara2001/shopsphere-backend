package com.shopsphere.securityTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn401WhenAccessingAdminEndpointWithoutToken()
            throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@shopsphere.com", roles = "CUSTOMER")
    void shouldReturn403WhenAccessingAdminEndpointWithCustomerToken()
        throws Exception{
        mockMvc.perform(get("/api/v1/admin/users/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = "ADMIN")
    void shouldReturn200WhenAccessingAdminEndpointWithAdminToken()
            throws Exception{
        mockMvc.perform(get("/api/v1/admin/users/all"))
                .andExpect(status().isOk());
    }
}
