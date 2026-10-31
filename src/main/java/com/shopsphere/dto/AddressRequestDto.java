package com.shopsphere.dto;

import com.shopsphere.constant.AddressType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressRequestDto {
    @NotBlank(groups = OnCreate.class, message = "Full name cannot be blank")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z .'-]*$",
            message = "Full name can contain only letters, spaces, dots, apostrophes and hyphens")
    private String fullName;

    @NotBlank(groups = OnCreate.class, message = "Phone number cannot be blank")
    @Pattern(regexp = "^[6-9]\\d{9}$",
            message = "Phone number must be a valid 10-digit mobile number starting with 6-9")
    private String phoneNumber;

    @NotBlank(groups = OnCreate.class, message = "House number cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9 /,.#-]+$",
            message = "House number contains invalid characters")
    private String houseNo;

    @NotBlank(groups = OnCreate.class, message = "Street cannot be blank")
    private String street;

    @Size(max = 100, message = "Landmark must not exceed 100 characters")
    private String landmark;

    @NotBlank(groups = OnCreate.class, message = "City cannot be blank")
    private String city;

    @NotBlank(groups = OnCreate.class, message = "State cannot be blank")
    private String state;

    @NotBlank(groups = OnCreate.class, message = "Country cannot be blank")
    private String country;

    @NotBlank(groups = OnCreate.class, message = "Postal code cannot be blank")
    @Pattern(regexp = "^[1-9]\\d{5}$",
            message = "Postal code must be a valid 6-digit PIN code")
    private String postalCode;

    @NotNull(groups = OnCreate.class, message = "Address type is required")
    private AddressType addressType;

    private Boolean defaultAddress;
}
