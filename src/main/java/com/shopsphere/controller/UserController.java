package com.shopsphere.controller;

import com.shopsphere.dto.OnCreate;
import com.shopsphere.dto.UserRequestDto;
import com.shopsphere.dto.UserResponseDto;
import com.shopsphere.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class UserController {
    private final UserService userService;

    @PostMapping("/auth/register")
    public ResponseEntity<UserResponseDto> registerUser(
            @Validated({Default.class, OnCreate.class}) @RequestBody UserRequestDto userRequestDto){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.registerUser(userRequestDto));
    }
    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long userId,@Valid @RequestBody UserRequestDto userRequestDto){
        return ResponseEntity.ok(userService.updateUser(userId,userRequestDto));
    }
}
