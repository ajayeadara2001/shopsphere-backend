package com.shopsphere.service.serviceImpl;

import com.shopsphere.dto.AddressRequestDto;
import com.shopsphere.dto.AddressResponseDto;
import com.shopsphere.entity.Address;
import com.shopsphere.entity.User;
import com.shopsphere.mapper.AddressMapper;
import com.shopsphere.mapper.UserMapper;
import com.shopsphere.repository.AddressRepository;
import com.shopsphere.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AddressResponseDto addAddress(AddressRequestDto addressRequestDto) {
        User currentUser=userMapper.getCurrentUser();
        Address newAddress=new Address();
              newAddress.setFullName(addressRequestDto.getFullName());
              newAddress.setPhoneNumber(addressRequestDto.getPhoneNumber());
              newAddress.setHouseNo(addressRequestDto.getHouseNo());
              newAddress.setStreet(addressRequestDto.getStreet());
              newAddress.setLandmark(addressRequestDto.getLandmark());
              newAddress.setCity(addressRequestDto.getCity());
              newAddress.setState(addressRequestDto.getState());
              newAddress.setCountry(addressRequestDto.getCountry());
              newAddress.setPostalCode(addressRequestDto.getPostalCode());
              newAddress.setAddressType(addressRequestDto.getAddressType());
              newAddress.setDefaultAddress(addressRequestDto.getDefaultAddress());
        List<Address> addressList=addressRepository.findByUser(currentUser);
        if(addressList.isEmpty()){
            newAddress.setDefaultAddress(true);
        }else if(Boolean.TRUE.equals((addressRequestDto.getDefaultAddress()))){
            for(Address existingAddress:addressList){
                existingAddress.setDefaultAddress(false);
            }
            addressRepository.saveAll(addressList);
        }else{
            newAddress.setDefaultAddress(false);
        }
        newAddress.setUser(currentUser);
        return addressMapper.mapAddressToResponse(addressRepository.save(newAddress));
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponseDto getAddressById(Long addressId) {
        User currentUser=userMapper.getCurrentUser();
        Address address=addressMapper.getAddressForUser(addressId,currentUser);
        return addressMapper.mapAddressToResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponseDto> getAllUserAddresses() {
        User currentUser=userMapper.getCurrentUser();
        List<Address> addressList=addressRepository.findByUser(currentUser);
        List<AddressResponseDto> addressResponseDtoList =new ArrayList<>();
        for(Address address:addressList){
            addressResponseDtoList.add(addressMapper.mapAddressToResponse(address));
        }
        return addressResponseDtoList;
    }

    @Override
    @Transactional
    public AddressResponseDto updateAddress(Long addressId, AddressRequestDto addressRequestDto) {
        User currentUser=userMapper.getCurrentUser();
        Address existingAddress=addressMapper.getAddressForUser(addressId,currentUser);
        Boolean currentDefault=Boolean.TRUE.equals(existingAddress.getDefaultAddress());
        if(addressRequestDto.getFullName()!=null){
            existingAddress.setFullName(addressRequestDto.getFullName());
        }
        if(addressRequestDto.getPhoneNumber()!=null){
            existingAddress.setPhoneNumber(addressRequestDto.getPhoneNumber());
        }
        if(addressRequestDto.getHouseNo()!=null){
            existingAddress.setHouseNo(addressRequestDto.getHouseNo());
        }
        if(addressRequestDto.getStreet()!=null){
            existingAddress.setStreet(addressRequestDto.getStreet());
        }
        if(addressRequestDto.getLandmark()!=null){
            existingAddress.setLandmark(addressRequestDto.getLandmark());
        }
        if(addressRequestDto.getCity()!=null){
            existingAddress.setCity(addressRequestDto.getCity());
        }
        if(addressRequestDto.getState()!=null){
            existingAddress.setState(addressRequestDto.getState());
        }
        if(addressRequestDto.getCountry()!=null){
            existingAddress.setCountry(addressRequestDto.getCountry());
        }
        if(addressRequestDto.getPostalCode()!=null){
            existingAddress.setPostalCode(addressRequestDto.getPostalCode());
        }
        if(addressRequestDto.getAddressType()!=null){
            existingAddress.setAddressType(addressRequestDto.getAddressType());
        }
        if(addressRequestDto.getDefaultAddress()!=null){
            existingAddress.setDefaultAddress(addressRequestDto.getDefaultAddress());
        }
        List<Address> addressList=addressRepository.findByUser(currentUser);
        if(Boolean.TRUE.equals((addressRequestDto.getDefaultAddress()))){
            for(Address address:addressList){
                address.setDefaultAddress(false);
            }
            addressRepository.saveAll(addressList);
            existingAddress.setDefaultAddress(true);
        }else{
            existingAddress.setDefaultAddress(currentDefault);
        }
        existingAddress.setUser(currentUser);
        return addressMapper.mapAddressToResponse(addressRepository.save(existingAddress));
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId) {
        User currentUser=userMapper.getCurrentUser();
        Address existingAddress=addressMapper.getAddressForUser(addressId,currentUser);
        boolean isDefault=Boolean.TRUE.equals(existingAddress.getDefaultAddress());
        addressRepository.delete(existingAddress);
        if(isDefault){
            List<Address> remainingAddresses =addressRepository.findByUser(currentUser);
            if(!remainingAddresses.isEmpty()){
                Address newDefault=remainingAddresses.getFirst();
                newDefault.setDefaultAddress(true);
                addressRepository.save(newDefault);
            }
        }
    }
}
