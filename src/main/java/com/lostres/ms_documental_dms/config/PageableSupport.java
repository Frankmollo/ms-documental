package com.lostres.ms_documental_dms.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableSupport {

    private PageableSupport() {
    }

    public static Pageable of(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? Math.min(size, 100) : 20;
        return PageRequest.of(safePage, safeSize, parseSort(sort));
    }

    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "uploadedAt");
        }
        String[] parts = sort.split(",", 2);
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
