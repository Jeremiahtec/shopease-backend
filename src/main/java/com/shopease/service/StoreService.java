package com.shopease.service;

import com.shopease.dto.store.StoreRequest;
import com.shopease.dto.store.StoreResponse;
import com.shopease.entity.Store;
import com.shopease.exception.ConflictException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.Mappers;
import com.shopease.repository.StoreRepository;
import com.shopease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @Transactional
    public StoreResponse create(Long vendorId, StoreRequest req) {
        if (storeRepository.existsByOwnerId(vendorId)) {
            throw new ConflictException("You already have a store. Use PUT /api/stores/{id} to update it.");
        }
        Store store = new Store();
        store.setOwner(userRepository.getReferenceById(vendorId));
        apply(store, req);
        return Mappers.toStore(storeRepository.save(store));
    }

    @Transactional(readOnly = true)
    public StoreResponse getById(Long id) {
        return Mappers.toStore(find(id));
    }

    @Transactional(readOnly = true)
    public StoreResponse getMine(Long vendorId) {
        Store store = storeRepository.findByOwnerId(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("You have not created a store yet"));
        return Mappers.toStore(store);
    }

    @Transactional
    public StoreResponse update(Long vendorId, Long storeId, StoreRequest req) {
        Store store = find(storeId);
        if (!store.getOwner().getId().equals(vendorId)) {
            throw new AccessDeniedException("You can only manage your own store");
        }
        apply(store, req);
        return Mappers.toStore(storeRepository.save(store));
    }

    private void apply(Store store, StoreRequest req) {
        store.setName(req.name().trim());
        store.setDescription(req.description());
        store.setLogoUrl(req.logoUrl());
    }

    private Store find(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id " + id));
    }
}
