package com.shopsphere.securityTest;

import com.shopsphere.security.CustomerUserDetailsService;
import com.shopsphere.security.JWTAuthenticationEntryPoint;
import com.shopsphere.security.JWTAuthenticationFilter;
import com.shopsphere.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class JWTAuthenticationFilterTest {
    @Mock
    private JwtService jwtService;
    @Mock
    private CustomerUserDetailsService customerUserDetailsService;
    @InjectMocks
    private JWTAuthenticationFilter jwtAuthenticationFilter;
    @Mock
    private JWTAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @BeforeEach
    void setUp(){
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_shouldContinueChainWhenAuthorizationHeaderIsMissing()
            throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        jwtAuthenticationFilter.doFilter(request,response,filterChain);
        verify(filterChain, times(1))
                .doFilter(request, response);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(customerUserDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_shouldContinueChainWhenBearerTokenIsMissing()
            throws ServletException,IOException{
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization","None1234");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain=mock(FilterChain.class);
        jwtAuthenticationFilter.doFilter(request,response,filterChain);
        verify(filterChain,times(1))
                .doFilter(request,response);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(customerUserDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_shouldAuthenticateUserWithValidToken()
            throws ServletException, IOException {
        String email = "customer@shopsphere.com";
        String token = "valid.jwt.token";
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain filterChain = mock(FilterChain.class);
        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .roles("CUSTOMER")
                .build();

        when(jwtService.extractEmail(token)).thenReturn(email);
        when(customerUserDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email,SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());

        verify(filterChain, times(1))
                .doFilter(request, response);
        verify(jwtService).extractEmail(token);
        verify(customerUserDetailsService).loadUserByUsername(email);
        verify(jwtService).isTokenValid(token, userDetails);

        verifyNoInteractions(jwtAuthenticationEntryPoint);
    }

    @Test
    void doFilter_shouldRejectInvalidToken()
        throws ServletException,IOException{
        String token="invalid.jwt.token";
        MockHttpServletRequest request=new MockHttpServletRequest();
        request.addHeader("Authorization","Bearer "+token);
        MockHttpServletResponse response=new MockHttpServletResponse();
        FilterChain filterChain=mock(FilterChain.class);
        when(jwtService.extractEmail(token))
                .thenThrow(new MalformedJwtException("Invalid token"));
        jwtAuthenticationFilter.doFilter(request,response,filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).extractEmail(token);
        verify(jwtAuthenticationEntryPoint).commence(
                eq(request),
                eq(response),
                any(BadCredentialsException.class));
        verify(filterChain,never()).doFilter(request,response);
    }

    @Test
    void doFilter_shouldRejectExpiredToken()
            throws ServletException,IOException{
        String token="expired.jwt.token";
        MockHttpServletRequest request=new MockHttpServletRequest();
        request.addHeader("Authorization","Bearer "+token);
        MockHttpServletResponse response=new MockHttpServletResponse();
        FilterChain filterChain=mock(FilterChain.class);
        when(jwtService.extractEmail(token))
                .thenThrow(new ExpiredJwtException(
                        null,
                        null,
                        "Jwt Expired"));
        jwtAuthenticationFilter.doFilter(request,response,filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).extractEmail(token);
        verify(jwtAuthenticationEntryPoint).commence(
                eq(request),
                eq(response),
                any(BadCredentialsException.class));
        verify(filterChain,never()).doFilter(request,response);
    }

    @Test
    void doFilter_shouldNotAuthenticateWhenTokenIsInvalidForUser()
            throws ServletException, IOException {
        String email = "customer1@shopsphere.com";
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        UserDetails userDetails = User.builder()
                .username("customer2@shopsphere.com")
                .password("password")
                .roles("CUSTOMER")
                .build();
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(customerUserDetailsService.loadUserByUsername(email))
                .thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails))
                .thenReturn(false);
        jwtAuthenticationFilter.doFilter(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).extractEmail(token);
        verify(customerUserDetailsService).loadUserByUsername(email);
        verify(jwtService).isTokenValid(token, userDetails);
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtAuthenticationEntryPoint);
    }

    @Test
    void doFilter_shouldNotOverwriteExistingAuthentication()
            throws ServletException, IOException {

        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        var existingAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "admin@shopsphere.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);
        jwtAuthenticationFilter.doFilter(request, response, filterChain);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        assertEquals(
                "admin@shopsphere.com",
                SecurityContextHolder.getContext().getAuthentication().getName()
        );

        assertEquals(
                "ROLE_ADMIN",
                SecurityContextHolder.getContext().getAuthentication()
                        .getAuthorities().iterator().next().getAuthority()
        );
        verify(jwtService).extractEmail(token);
        verifyNoMoreInteractions(jwtService);
        verifyNoInteractions(customerUserDetailsService);
        verifyNoInteractions(jwtAuthenticationEntryPoint);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldContinueChainWhenEmailIsNull()
        throws ServletException, IOException {

        String token="valid.jwt.token";
        MockHttpServletRequest request=new MockHttpServletRequest();
        MockHttpServletResponse response=new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain filterChain=mock(FilterChain.class);
        when(jwtService.extractEmail(token)).thenReturn(null);
        jwtAuthenticationFilter.doFilter(request,response,filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).extractEmail(token);
        verifyNoInteractions(customerUserDetailsService);
        verifyNoInteractions(jwtAuthenticationEntryPoint);
        verify(filterChain).doFilter(request,response);
    }

    @Test
    void doFilter_shouldRejectTokenWhenUserIsNotFound()
            throws ServletException, IOException {
        String token="valid.jwt.token";
        String email="customer@shopsphere.com";
        MockHttpServletRequest request=new MockHttpServletRequest();
        MockHttpServletResponse response=new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain filterChain=mock(FilterChain.class);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(customerUserDetailsService.loadUserByUsername(email))
                .thenThrow(new UsernameNotFoundException("User not found."));

        jwtAuthenticationFilter.doFilter(request,response,filterChain);
        verify(jwtService).extractEmail(token);
        verify(customerUserDetailsService).loadUserByUsername(email);
        verify(jwtAuthenticationEntryPoint).commence(
                eq(request),
                eq(response),
                any(BadCredentialsException.class)
        );
        verify(filterChain,never()).doFilter(request,response);
    }
}
