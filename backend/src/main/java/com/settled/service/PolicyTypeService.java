package com.settled.service;

import com.settled.domain.PolicyType;
import com.settled.dto.policy.PolicyTypeRequest;
import com.settled.dto.policy.PolicyTypeResponse;
import com.settled.exception.BadRequestException;
import com.settled.exception.ResourceNotFoundException;
import com.settled.repository.PolicyTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyTypeService {

    private final PolicyTypeRepository policyTypeRepository;

    @Cacheable(cacheNames = "policyTypes")
    @Transactional(readOnly = true)
    public List<PolicyTypeResponse> listActive() {
        return policyTypeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(PolicyTypeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PolicyTypeResponse> listAll() {
        return policyTypeRepository.findAll().stream()
                .map(PolicyTypeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PolicyTypeResponse get(UUID id) {
        return PolicyTypeResponse.from(getEntity(id));
    }

    @Transactional
    @CacheEvict(cacheNames = "policyTypes", allEntries = true)
    public PolicyTypeResponse create(PolicyTypeRequest request) {
        if (policyTypeRepository.existsByCode(request.code().toUpperCase())) {
            throw new BadRequestException("Policy type with code " + request.code() + " already exists");
        }
        return PolicyTypeResponse.from(policyTypeRepository.save(PolicyTypeRequest.toEntity(request)));
    }

    @Transactional
    @CacheEvict(cacheNames = "policyTypes", allEntries = true)
    public PolicyTypeResponse update(UUID id, PolicyTypeRequest request) {
        PolicyType type = getEntity(id);
        policyTypeRepository.findByCode(request.code().toUpperCase())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Policy type with code " + request.code() + " already exists");
                });
        type.setCode(request.code().toUpperCase());
        type.setName(request.name());
        type.setDescription(request.description());
        type.setCoverageAmount(request.coverageAmount());
        type.setPremiumRate(request.premiumRate());
        type.setActive(request.active());
        return PolicyTypeResponse.from(policyTypeRepository.save(type));
    }

    public PolicyType getEntity(UUID id) {
        return policyTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("PolicyType", id));
    }
}