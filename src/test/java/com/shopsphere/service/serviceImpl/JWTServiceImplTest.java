package com.shopsphere.service.serviceImpl;

import com.shopsphere.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JWTServiceImplTest {
    private JwtService jwtService;
    private String secret;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl();
        secret = Base64.getEncoder().encodeToString(
                "shopsphere-test-secret-key-must-be-at-least-32-bytes".getBytes());
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
    }

    @Test
    void generateToken_shouldGenerateValidToken() {
        String email = "customer@shopsphere.com";
        String token = jwtService.generateToken(email);
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(email, jwtService.extractEmail(token));
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidUser(){
        String email = "customer@shopsphere.com";
        String token= jwtService.generateToken(email);
        UserDetails userDetails=User.builder()
                .username(email)
                .password("password")
                .roles("CUSTOMER")
                .build();
        boolean result= jwtService.isTokenValid(token,userDetails);
        assertTrue(result);
    }

    @Test
    void isTokenValid_shouldReturnFalseForDifferentUser(){
        String email = "customer@shopsphere.com";
        String token= jwtService.generateToken(email);
        UserDetails userDetails=User.builder()
                .username("anothercustomer@shopsphere.com")
                .password("password")
                .roles("CUSTOMER")
                .build();
        boolean result= jwtService.isTokenValid(token,userDetails);
        assertFalse(result);
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken(){
        String email="customer@shopsphere.com";
        ReflectionTestUtils.setField(jwtService,"expiration",-1000L);
        String token= jwtService.generateToken(email);

        UserDetails userDetails=User.builder()
                .username(email)
                .password("password")
                .roles("CUSTOMER")
                .build();
        boolean result= jwtService.isTokenValid(token,userDetails);
        assertFalse(result);
    }

    @Test
    void isTokenValid_shouldReturnFalseForMalformedToken(){
        String malformedToken="this.is.not.a.valid.jwt";
        UserDetails userDetails=User.builder()
                .username(malformedToken)
                .password("password")
                .roles("CUSTOMER")
                .build();
        boolean result= jwtService.isTokenValid(malformedToken,userDetails);
        assertFalse(result);
    }

    @Test
    void isTokenValid_shouldReturnFalseForNullUserDetails(){
        String email="customer@shopsphere.com";
        String token= jwtService.generateToken(email);
        boolean result= jwtService.isTokenValid(token,null);
        assertFalse(result);
    }

    @Test
    void isTokenValid_shouldReturnFalseForTamperedToken(){
        String email="customer@shopsphere.com";
        String token= jwtService.generateToken(email);
        String tamperedToken=token.substring(0,token.length()-5)+"QWERTY";
        UserDetails userDetails=User.builder()
                .username(email)
                .password("password")
                .roles("CUSTOMER")
                .build();
        boolean result= jwtService.isTokenValid(tamperedToken,userDetails);
        assertFalse(result);
    }


}