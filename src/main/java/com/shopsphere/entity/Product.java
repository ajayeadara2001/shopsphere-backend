package com.shopsphere.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;
    private String productName;
    @Column(columnDefinition = "TEXT")
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String brand;
    private String imageUrl;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id",nullable = false)
    private Category category;
    @OneToMany(mappedBy = "product",orphanRemoval = true)
    private List<Review> reviews=new ArrayList<>();

    @PrePersist
    public void onCreate(){
        this.createdAt=LocalDateTime.now();
        this.updatedAt=LocalDateTime.now();
        this.active=true;
    }
    @PreUpdate
    public void onUpdate(){
        this.updatedAt=LocalDateTime.now();
    }

}
