package com.shopsphere.controller;

import com.shopsphere.dto.AddressRequestDto;
import com.shopsphere.dto.AddressResponseDto;
import com.shopsphere.dto.OnCreate;
import com.shopsphere.service.AddressService;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class AddressController {
    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressResponseDto> addAddress(
            @Validated({Default.class, OnCreate.class})
            @RequestBody AddressRequestDto addressRequestDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(
          addressService.addAddress(addressRequestDto)
        );
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponseDto> getAddressById(@PathVariable Long addressId){
        return ResponseEntity.ok(addressService.getAddressById(addressId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<AddressResponseDto>> getAllUserAddresses(){
        return ResponseEntity.ok(addressService.getAllUserAddresses());
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponseDto> getAddressById(
            @PathVariable Long addressId,
            @RequestBody AddressRequestDto addressRequestDto){
        return ResponseEntity.ok(addressService.updateAddress(addressId,addressRequestDto));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<AddressResponseDto> deleteAddress(@PathVariable Long addressId){
        addressService.deleteAddress(addressId);
        return ResponseEntity.noContent().build();
    }
}
