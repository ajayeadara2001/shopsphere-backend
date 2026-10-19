package com.shopsphere.service.serviceImpl;

import com.shopsphere.dto.CategoryRequestDto;
import com.shopsphere.dto.CategoryResponseDto;
import com.shopsphere.entity.Category;
import com.shopsphere.exception.CategoryNotFoundException;
import com.shopsphere.mapper.CategoryMapper;
import com.shopsphere.repository.CategoryRepository;
import com.shopsphere.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto) {
        Optional<Category> existingCategory=categoryRepository.findByCategoryName(categoryRequestDto.getCategoryName());
        if(existingCategory.isPresent()){
            Category category=existingCategory.get();
            if(category.getActive()) {
                throw new IllegalArgumentException("Category already exists.");
            }
            category.setActive(true);
            category.setDescription(categoryRequestDto.getDescription());
            return categoryMapper.mapCategoryToResponse(categoryRepository.save(category));
        }
        Category newCategory=new Category();
        newCategory.setCategoryName(categoryRequestDto.getCategoryName());
        newCategory.setDescription(categoryRequestDto.getDescription());
        newCategory.setActive(true);
        return categoryMapper.mapCategoryToResponse(categoryRepository.save(newCategory));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllActiveCategory() {
        List<Category> categoryList =categoryRepository.findByActiveTrue();
        List<CategoryResponseDto> categoryResponseDtoList=new ArrayList<>();
        for(Category category: categoryList){
            categoryResponseDtoList.add(categoryMapper.mapCategoryToResponse(category));
        }
        return categoryResponseDtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategory() {
        List<Category> categoryList =categoryRepository.findAll();
        List<CategoryResponseDto> categoryResponseDtoList=new ArrayList<>();
        for(Category category: categoryList){
            categoryResponseDtoList.add(categoryMapper.mapCategoryToResponse(category));
        }
        return categoryResponseDtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long categoryId) {
        Category existingCategory=categoryMapper.getCategory(categoryId);
        return categoryMapper.mapCategoryToResponse(existingCategory);
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long categoryId, CategoryRequestDto categoryRequestDto) {
        Category existingCategory=categoryMapper.getCategory(categoryId);
        existingCategory.setCategoryName(categoryRequestDto.getCategoryName());
        existingCategory.setDescription(categoryRequestDto.getDescription());
        return categoryMapper.mapCategoryToResponse(categoryRepository.save(existingCategory));
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        Category existingCategory=categoryMapper.getCategory(categoryId);
        if(!existingCategory.getActive().equals(true)){
            throw new IllegalArgumentException("Category is already inactive.");
        }
        existingCategory.setActive(false);
        categoryRepository.save(existingCategory);
    }

    @Override
    @Transactional
    public CategoryResponseDto activateCategory(Long categoryId) {
        Category existingCategory=categoryMapper.getCategory(categoryId);
        if(existingCategory.getActive().equals(true)){
            throw new IllegalArgumentException("Category is already active.");
        }
        existingCategory.setActive(true);
        return categoryMapper.mapCategoryToResponse(categoryRepository.save(existingCategory));
    }
}
