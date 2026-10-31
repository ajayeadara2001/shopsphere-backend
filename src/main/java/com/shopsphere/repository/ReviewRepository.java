package com.shopsphere.repository;

import com.shopsphere.entity.Product;
import com.shopsphere.entity.Review;
import com.shopsphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review,Long> {
    List<Review> findByProduct(Product product);
    Optional<Review> findByUserAndProduct(User user,Product product);
    @Query("SELECT AVG(r.rating) FROM Review  r WHERE r.product.productId=:productId")
    Double findByAverageRatingByProductId(Long productId);
    long countByProduct(Product product);
}
