package com.shopsphere.service;

import com.shopsphere.dto.ProductRequestDto;
import com.shopsphere.dto.ProductResponseDto;

import java.util.List;

public interface ProductService {
    ProductResponseDto createProduct(ProductRequestDto productRequestDto);
    List<ProductResponseDto> getAllActiveProducts();
    List<ProductResponseDto> getAllProducts();
    ProductResponseDto getProductById(Long productId);
    ProductResponseDto updateProduct(Long productId,ProductRequestDto productRequestDto);
    void deleteProduct(Long productId);
    ProductResponseDto activateProduct(Long productId);
}
