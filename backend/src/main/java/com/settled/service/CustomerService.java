package com.settled.service;

import com.settled.domain.Customer;
import com.settled.domain.User;
import com.settled.dto.customer.CustomerProfileRequest;
import com.settled.dto.customer.CustomerResponse;
import com.settled.exception.ResourceNotFoundException;
import com.settled.repository.CustomerRepository;
import com.settled.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CustomerResponse getMyProfile(UUID userId) {
        return CustomerResponse.from(getCustomer(userId));
    }

    @Transactional
    public CustomerResponse updateMyProfile(UUID userId, CustomerProfileRequest request) {
        Customer customer = getCustomer(userId);
        if (request.address() != null) customer.setAddress(request.address());
        if (request.city() != null) customer.setCity(request.city());
        if (request.state() != null) customer.setState(request.state());
        if (request.postalCode() != null) customer.setPostalCode(request.postalCode());
        if (request.country() != null) customer.setCountry(request.country());
        if (request.phone() != null) {
            User user = customer.getUser();
            user.setPhone(request.phone());
            userRepository.save(user);
        }
        return CustomerResponse.from(customerRepository.save(customer));
    }

    public Customer getCustomer(UUID userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + userId));
    }
}