package com.shopease.service;

import com.shopease.dto.category.CategoryRequest;
import com.shopease.dto.category.CategoryResponse;
import com.shopease.dto.common.PageResponse;
import com.shopease.entity.Category;
import com.shopease.exception.ConflictException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.Mappers;
import com.shopease.repository.CategoryRepository;
import com.shopease.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> list(int page, int size) {
        Pageable pageable = PageUtil.of(page, size, null, "asc", Set.of("name"), "name");
        return PageResponse.from(categoryRepository.findAll(pageable).map(Mappers::toCategory));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        String name = req.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A category named '" + name + "' already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(req.description());
        return Mappers.toCategory(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));
        String name = req.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("A category named '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(req.description());
        return Mappers.toCategory(categoryRepository.save(category));
    }
}
