package com.shopsphere.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;


public class JWTAuthenticationEntryPointTest {
    private final ObjectMapper objectMapper=new ObjectMapper();
    private final JWTAuthenticationEntryPoint jwtAuthenticationEntryPoint=
            new JWTAuthenticationEntryPoint(objectMapper);

    @Test
    void commence_shouldReturnUnauthorizedJsonResponse() throws Exception {
        MockHttpServletResponse response=new MockHttpServletResponse();
        MockHttpServletRequest request=new MockHttpServletRequest();
        request.setRequestURI("/api/products");
        BadCredentialsException badCredentialsException=
                new BadCredentialsException("Invalid JWT");
        jwtAuthenticationEntryPoint.commence(request,response,badCredentialsException);
        assertEquals(401,response.getStatus());
        assertEquals("application/json",response.getContentType());
        Map<String, Object> responseBody =objectMapper.readValue(
                        response.getContentAsString(),
                        Map.class);
        assertEquals(401, responseBody.get("status"));
        assertEquals("Unauthorized", responseBody.get("error"));
        assertEquals("Invalid JWT", responseBody.get("message"));
        assertEquals("/api/products", responseBody.get("path"));
        assertNotNull(responseBody.get("timestamp"));
    }

    @Test
    void commence_shouldReturnActualAuthenticationExceptionMessage() throws Exception {
        MockHttpServletRequest request=new MockHttpServletRequest();
        MockHttpServletResponse response=new MockHttpServletResponse();
        request.setRequestURI("/api/orders");
        BadCredentialsException badCredentialsException=
                new BadCredentialsException("Authentication token is expired");
        jwtAuthenticationEntryPoint.commence(request,response,badCredentialsException);
        Map<String, Object> responseBody=objectMapper.readValue(
                response.getContentAsString(),
                Map.class);
        assertEquals("Authentication token is expired",responseBody.get("message"));
        assertEquals("/api/orders",responseBody.get("path"));
        assertEquals(401,response.getStatus());

    }
}
