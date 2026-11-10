package com.example.demo.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(List<T> items, Pagination pagination) {
    public record Pagination(int page, int limit, long totalItems, int totalPages) { }

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                new Pagination(page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages())
        );
    }
}
