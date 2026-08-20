package com.settled.dto.customer;

import com.settled.domain.Customer;

import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        UUID userId,
        String customerNumber,
        LocalDate dateOfBirth,
        String address,
        String city,
        String state,
        String postalCode,
        String country
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getUser().getId(),
                customer.getCustomerNumber(),
                customer.getDateOfBirth(),
                customer.getAddress(),
                customer.getCity(),
                customer.getState(),
                customer.getPostalCode(),
                customer.getCountry()
        );
    }
}