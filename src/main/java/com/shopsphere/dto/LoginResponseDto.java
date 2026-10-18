package com.shopsphere.dto;

import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponseDto {
    private String message;
    private String token;
    private String tokenType;

}
