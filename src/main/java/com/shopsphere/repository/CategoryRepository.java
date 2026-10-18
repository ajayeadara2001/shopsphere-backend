package com.shopsphere.repository;

import com.shopsphere.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category,Long> {
    List<Category> findByActiveTrue();
    Optional<Category> findByCategoryName(String categoryName);
}
