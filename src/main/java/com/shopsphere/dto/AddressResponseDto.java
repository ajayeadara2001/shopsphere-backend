package com.shopsphere.dto;

import com.shopsphere.constant.AddressType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressResponseDto {
    private Long addressId;
    private String fullName;
    private String phoneNumber;
    private String houseNo;
    private String street;
    private String landmark;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private AddressType addressType;
    private Boolean defaultAddress;
}
