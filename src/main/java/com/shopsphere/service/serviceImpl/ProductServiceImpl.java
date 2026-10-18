package com.shopsphere.service.serviceImpl;

import com.shopsphere.dto.ProductRequestDto;
import com.shopsphere.dto.ProductResponseDto;
import com.shopsphere.entity.Category;
import com.shopsphere.entity.Product;
import com.shopsphere.exception.CategoryNotFoundException;
import com.shopsphere.exception.ProductNotFoundException;
import com.shopsphere.mapper.ProductMapper;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.repository.ProductRepository;
import com.shopsphere.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    public Product getProduct(Long productId){
        return productRepository.findByProductIdAndActiveTrue(productId)
                .orElseThrow(()-> new ProductNotFoundException(
                        "Product not found for Id :"+productId));
    }
    public Category getCategory(Long categoryId){
        return categoryRepository.findById(categoryId)
                .orElseThrow(()-> new CategoryNotFoundException(
                        "Category not found for Id: "+categoryId));
    }

    @Override
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        Category category=getCategory(productRequestDto.getCategoryId());
        if(!category.getActive().equals(true)){
            throw new IllegalArgumentException("Product cannot be created under an inactive category.");
        }
        Product newProduct=new Product();
        newProduct.setProductName(productRequestDto.getProductName());
        newProduct.setDescription(productRequestDto.getDescription());
        newProduct.setPrice(productRequestDto.getPrice());
        newProduct.setStockQuantity(productRequestDto.getStockQuantity());
        newProduct.setBrand(productRequestDto.getBrand());
        newProduct.setImageUrl(productRequestDto.getImageUrl());
        newProduct.setActive(true);
        newProduct.setCategory(category);
        return productMapper.mapProductToResponse(productRepository.save(newProduct));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllActiveProducts() {
        List<Product> productList=productRepository.findByActiveTrue();
        List<ProductResponseDto> productResponseDtoList=new ArrayList<>();
        for(Product product:productList){
            productResponseDtoList.add(productMapper.mapProductToResponse(product));
        }
        return productResponseDtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        List<Product> productList=productRepository.findAll();
        List<ProductResponseDto> productResponseDtoList=new ArrayList<>();
        for(Product product:productList){
            productResponseDtoList.add(productMapper.mapProductToResponse(product));
        }
        return productResponseDtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long productId) {
        Product existingProduct=getProduct(productId);
        return productMapper.mapProductToResponse(existingProduct);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long productId, ProductRequestDto productRequestDto) {
        Category category=getCategory(productRequestDto.getCategoryId());
        if(!category.getActive().equals(true)){
            throw new IllegalArgumentException("Product cannot be assigned under an inactive category.");
        }
        Product existingProduct=getProduct(productId);
        if(productRequestDto.getProductName()!=null){
            existingProduct.setProductName(productRequestDto.getProductName());
        }
        if(productRequestDto.getDescription()!=null) {
            existingProduct.setDescription(productRequestDto.getDescription());
        }
        if(productRequestDto.getPrice()!=null) {
            existingProduct.setPrice(productRequestDto.getPrice());
        }
        if(productRequestDto.getStockQuantity()!=null) {
            existingProduct.setStockQuantity(productRequestDto.getStockQuantity());
        }
        if(productRequestDto.getBrand()!=null) {
            existingProduct.setBrand(productRequestDto.getBrand());
        }
        if(productRequestDto.getImageUrl()!=null) {
            existingProduct.setImageUrl(productRequestDto.getImageUrl());
        }
        return productMapper.mapProductToResponse(productRepository.save(existingProduct));
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product existingProduct=getProduct(productId);
        if(!existingProduct.getActive().equals(true)){
            throw new IllegalArgumentException("Product is already inactive.");
        }
        existingProduct.setActive(false);
        productMapper.mapProductToResponse(productRepository.save(existingProduct));
    }

    @Override
    @Transactional
    public ProductResponseDto activateProduct(Long productId) {
        Product existingProduct=getProduct(productId);
        if(!existingProduct.getCategory().getActive().equals(true)){
            throw new IllegalArgumentException("Product cannot be activated because it's category is inactive.");
        }
        if(existingProduct.getActive().equals(true)){
            throw new IllegalArgumentException("Product is already active.");
        }
        existingProduct.setActive(true);
        return productMapper.mapProductToResponse(productRepository.save(existingProduct));
    }
}
