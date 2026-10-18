package com.shopsphere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRequestDto {
    @NotBlank(groups = OnCreate.class,message = "Username cannot be blank")
    private String userName;

    @NotBlank(groups = OnCreate.class,message = "Email cannot be blank")
    @Email(message = "Please provide a valid email address")
    @Pattern(
            regexp = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$",
            message = "Email must include a valid domain and small characters, e.g. name@example.com"
    )
    private String email;

    @NotBlank(groups = OnCreate.class,message = "Phone number cannot be blank")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Phone number must be a valid 10-digit mobile number starting with 6-9"
    )
    private String phoneNo;

    @NotBlank(groups = OnCreate.class,message = "Password cannot be blank")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-]).+$",
            message = "Password must contain an uppercase letter, a lowercase letter, a digit and a special character"
    )
    private String password;
}
