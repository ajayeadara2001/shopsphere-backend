package com.shopsphere.service;

import com.shopsphere.dto.CategoryRequestDto;
import com.shopsphere.dto.CategoryResponseDto;

import java.util.List;

public interface CategoryService {
    CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto);
    List<CategoryResponseDto> getAllActiveCategory();
    List<CategoryResponseDto> getAllCategory();
    CategoryResponseDto getCategoryById(Long categoryId);
    CategoryResponseDto updateCategory(Long categoryId, CategoryRequestDto categoryRequestDto);
    void deleteCategory(Long categoryId);
    CategoryResponseDto activateCategory(Long categoryId);
}
