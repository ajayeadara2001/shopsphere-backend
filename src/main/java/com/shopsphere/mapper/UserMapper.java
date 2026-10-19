package com.shopsphere.mapper;

import com.shopsphere.dto.UserResponseDto;
import com.shopsphere.entity.User;
import com.shopsphere.exception.UserNotFoundException;
import com.shopsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserMapper {
    private final UserRepository userRepository;
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
    public User getCurrentUser(){
        Authentication authentication= SecurityContextHolder.getContext().getAuthentication();
        String email= authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(()-> new UserNotFoundException("User not found"));
    }

    public User getUser(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(()-> new UserNotFoundException("User not found for Id: "+userId));
    }
}
