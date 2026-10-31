package com.shopsphere.mapper;

import com.shopsphere.dto.AddressResponseDto;
import com.shopsphere.entity.Address;
import com.shopsphere.entity.User;
import com.shopsphere.exception.AddressException;
import com.shopsphere.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddressMapper {
    private final AddressRepository addressRepository;
    public AddressResponseDto mapAddressToResponse(Address address){
        return new AddressResponseDto(
                address.getAddressId(),
                address.getFullName(),
                address.getPhoneNumber(),
                address.getHouseNo(),
                address.getStreet(),
                address.getLandmark(),
                address.getCity(),
                address.getState(),
                address.getCountry(),
                address.getPostalCode(),
                address.getAddressType(),
                address.getDefaultAddress()
        );
    }

    public Address getAddressForUser(Long addressId, User user){
        return addressRepository.findByAddressIdAndUser(addressId,user)
                .orElseThrow(()-> new AddressException("Address not found for Id: "+addressId));
    }
}
