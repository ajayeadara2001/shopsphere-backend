package com.shopsphere.service;

import com.shopsphere.dto.LoginRequestDto;
import com.shopsphere.dto.LoginResponseDto;

public interface LoginService {
    LoginResponseDto login(LoginRequestDto loginRequestDto);
}
