package com.shopsphere.entity;

import com.shopsphere.constant.AddressType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @Column(name = "address_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;
    private String fullName;
    private String phoneNumber;
    private String houseNo;
    private String street;
    private String landmark;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Enumerated(EnumType.STRING)
    private AddressType addressType;
    private Boolean defaultAddress;
}
