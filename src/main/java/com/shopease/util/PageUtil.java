package com.shopease.util;

import com.shopease.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageUtil {

    public static final int MAX_PAGE_SIZE = 100;

    private PageUtil() {
    }

    /** Builds a safe Pageable: clamps page/size and only allows sorting on whitelisted fields. */
    public static Pageable of(int page, int size, String sortBy, String direction,
                              Set<String> allowedSortFields, String defaultSortField) {
        String field = defaultSortField;
        if (sortBy != null && !sortBy.isBlank()) {
            if (!allowedSortFields.contains(sortBy)) {
                throw new BadRequestException("Invalid sortBy '" + sortBy + "'. Allowed values: " + allowedSortFields);
            }
            field = sortBy;
        }
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(dir, field));
    }

    /** Newest-first pagination on createdAt. */
    public static Pageable newestFirst(int page, int size) {
        return of(page, size, null, "desc", Set.of("createdAt"), "createdAt");
    }
}
