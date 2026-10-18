package com.shopsphere.controller;

import com.shopsphere.dto.LoginRequestDto;
import com.shopsphere.dto.LoginResponseDto;
import com.shopsphere.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class LoginController {
    private final LoginService loginService;

    @PostMapping("/login/me")
    public ResponseEntity<LoginResponseDto> userLogin(
            @RequestBody LoginRequestDto loginRequestDto){
        return ResponseEntity.ok(loginService.login(loginRequestDto));
    }
}
