package com.shopsphere.service;

import com.shopsphere.dto.AddressRequestDto;
import com.shopsphere.dto.AddressResponseDto;

import java.util.List;

public interface AddressService {
    AddressResponseDto addAddress(AddressRequestDto addressRequestDto);
    AddressResponseDto getAddressById(Long addressId);
    List<AddressResponseDto> getAllUserAddresses();
    AddressResponseDto updateAddress(Long addressId, AddressRequestDto addressRequestDto);
    void deleteAddress(Long addressId);
}
