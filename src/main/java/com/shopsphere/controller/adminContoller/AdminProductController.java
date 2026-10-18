package com.shopsphere.controller.adminContoller;

import com.shopsphere.dto.OnCreate;
import com.shopsphere.dto.ProductRequestDto;
import com.shopsphere.dto.ProductResponseDto;
import com.shopsphere.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(
            @Validated ({Default.class, OnCreate.class})
            @RequestBody ProductRequestDto productRequestDto){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(productRequestDto));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductResponseDto>> getAllProducts(){
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long productId, @Valid @RequestBody ProductRequestDto productRequestDto){
        return ResponseEntity.ok(productService.updateProduct(productId,productRequestDto));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId){
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/active")
    public ResponseEntity<ProductResponseDto> activateProduct(@PathVariable Long productId){
        return ResponseEntity.ok(productService.activateProduct(productId));
    }

}
