package com.shopsphere.controller;

import com.shopsphere.dto.ProductResponseDto;
import com.shopsphere.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    @GetMapping("/active/all")
    public ResponseEntity<List<ProductResponseDto>> getAllActiveProducts(){
        return ResponseEntity.ok(productService.getAllActiveProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductById(
            @PathVariable Long productId){
        return ResponseEntity.ok(productService.getProductById(productId));
    }
}
