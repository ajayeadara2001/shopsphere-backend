package com.shopsphere.mapper;

import com.shopsphere.dto.UserResponseDto;
import com.shopsphere.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserMapper {
    public UserResponseDto mapUserToTheResponse(User user){
        UserResponseDto userResponseDto=new UserResponseDto();
        userResponseDto.setUserId(user.getUserId());
        userResponseDto.setUserName(user.getUserName());
        userResponseDto.setEmail(user.getEmail());
        userResponseDto.setPhoneNo(user.getPhoneNo());
        userResponseDto.setRole(user.getRole());
        userResponseDto.setCreatedAt(user.getCreatedAt());
        return userResponseDto;
    }
}
