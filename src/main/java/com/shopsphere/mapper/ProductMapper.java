package com.shopsphere.mapper;

import com.shopsphere.dto.ProductResponseDto;
import com.shopsphere.entity.Product;
import com.shopsphere.exception.ProductNotFoundException;
import com.shopsphere.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMapper {
    private final ProductRepository productRepository;
    public ProductResponseDto mapProductToResponse(Product product){
        return new ProductResponseDto(
        product.getProductId(),
                product.getProductName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getBrand(),
                product.getImageUrl(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getCategory().getCategoryId(),
                product.getCategory().getCategoryName()
        );
    }
    public Product getProduct(Long productId) {
        return productRepository.findByProductIdAndActiveTrue(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found for Id : " + productId));
    }
}
