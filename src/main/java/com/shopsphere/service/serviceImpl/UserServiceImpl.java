package com.shopsphere.service.serviceImpl;

import com.shopsphere.constant.Role;
import com.shopsphere.dto.UserRequestDto;
import com.shopsphere.dto.UserResponseDto;
import com.shopsphere.entity.User;
import com.shopsphere.exception.AccessDeniedException;
import com.shopsphere.exception.EmailAlreadyPresentException;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponseDto registerUser(UserRequestDto userRequestDto) {

        Optional<User> existingUser=userRepository.findByEmail(userRequestDto.getEmail());
        if(existingUser.isPresent()) {
            throw new EmailAlreadyPresentException("Email Already Present: " + userRequestDto.getEmail());
        }
        User newUser=new User();
        newUser.setUserName(userRequestDto.getUserName());
        newUser.setEmail(userRequestDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
        newUser.setPhoneNo(userRequestDto.getPhoneNo());
        newUser.setRole(Role.CUSTOMER);
        return userMapper.mapUserToTheResponse(userRepository.save(newUser));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long userId) {
        User existingUser=userMapper.getUser(userId);
        return userMapper.mapUserToTheResponse(existingUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        List<User> userList =userRepository.findAll();
        List<UserResponseDto> userResponseDtoList=new ArrayList<>();
        for(User user : userList){
            userResponseDtoList.add(userMapper.mapUserToTheResponse(user));
        }
        return userResponseDtoList;
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long userId, UserRequestDto userRequestDto) {
        User user= userMapper.getCurrentUser();
        if(user.getRole()!= Role.ADMIN && !user.getUserId().equals(userId)){
            throw new AccessDeniedException("You are not authorized to update this user.");
        }
        User existingUser=userMapper.getUser(userId);
        Optional<User> userWithEmail=userRepository.findByEmail(userRequestDto.getEmail());
        if(userWithEmail.isPresent() && !userWithEmail.get().getUserId().equals(userId)){
            throw new EmailAlreadyPresentException("Email Already exists");
        }
        if(userRequestDto.getUserName()!=null){
            existingUser.setUserName(userRequestDto.getUserName());
        }
        if(userRequestDto.getEmail()!=null){
            existingUser.setEmail(userRequestDto.getEmail());
        }
        if(userRequestDto.getPassword()!=null){
            existingUser.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
        }
        if(userRequestDto.getPhoneNo()!=null){
            existingUser.setPhoneNo(userRequestDto.getPhoneNo());
        }
        return userMapper.mapUserToTheResponse(userRepository.save(existingUser));
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User existingUser=userMapper.getUser(userId);
        userRepository.delete(existingUser);
    }
}
