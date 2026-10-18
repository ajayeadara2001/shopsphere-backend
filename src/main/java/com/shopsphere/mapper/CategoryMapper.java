package com.shopsphere.mapper;

import com.shopsphere.dto.CategoryResponseDto;
import com.shopsphere.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryMapper {
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
}
