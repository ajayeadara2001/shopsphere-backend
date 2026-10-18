package com.shopsphere.service.serviceImpl;

import com.shopsphere.dto.LoginRequestDto;
import com.shopsphere.dto.LoginResponseDto;
import com.shopsphere.entity.User;
import com.shopsphere.exception.InvalidCredentialsException;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.JwtService;
import com.shopsphere.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        User existingUser=userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(()-> new InvalidCredentialsException("Invalid Email or Password"));
        if(!passwordEncoder.matches(loginRequestDto.getPassword(),existingUser.getPassword())){
            throw new InvalidCredentialsException("Invalid Email or Password");
        }
        String token=jwtService.generateToken(existingUser.getEmail());
        return LoginResponseDto.builder()
                .message("Login Successful, Welcome "+existingUser.getUserName())
                .token(token)
                .tokenType("Bearer")
                .build();
    }
}
