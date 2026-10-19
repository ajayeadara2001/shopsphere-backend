package com.shopsphere.mapper;

import com.shopsphere.dto.CategoryResponseDto;
import com.shopsphere.entity.Category;
import com.shopsphere.exception.CategoryNotFoundException;
import com.shopsphere.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryMapper {
    private final CategoryRepository categoryRepository;
    public CategoryResponseDto mapCategoryToResponse(Category category){
        return new CategoryResponseDto(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getDescription(),
                category.getActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public Category getCategory(Long categoryId){
        return categoryRepository.findById(categoryId)
                .orElseThrow(()-> new CategoryNotFoundException(
                        "Category not found for Id: "+categoryId));
    }
}
