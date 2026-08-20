package com.settled.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "customers", uniqueConstraints = @UniqueConstraint(columnNames = "customer_number"))
public class Customer extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 20)
    private String customerNumber;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(length = 120)
    private String address;

    @Column(length = 80)
    private String city;

    @Column(length = 40)
    private String state;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 40)
    private String country;
}