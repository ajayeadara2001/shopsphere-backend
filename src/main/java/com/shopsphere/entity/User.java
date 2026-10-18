package com.shopsphere.entity;

import com.shopsphere.constant.Roles;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class User {
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String userName;
    @Column(unique = true, nullable = false)
    private String email;
    private String password;
    private String phoneNo;
    @Enumerated(EnumType.STRING)
    private Roles role;
    private LocalDateTime createdAt;


    @PrePersist
    public void onCreate(){
        this.createdAt=LocalDateTime.now();
    }
}
