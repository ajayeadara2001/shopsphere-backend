package com.shopsphere.mapper;

import com.shopsphere.dto.ProductResponseDto;
import com.shopsphere.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
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
}
