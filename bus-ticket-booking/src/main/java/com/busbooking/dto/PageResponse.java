package com.busbooking.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a list. {@code page} starts at 1. {@code from}/{@code to} are the positions of the
 * first and last item on this page, ready for "Showing 21–40 of 156" ({@code 0} when empty).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long from,
        long to,
        List<String> sort
) {
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return of(page, page.getContent().stream().map(mapper).toList());
    }

    public static <E, T> PageResponse<T> of(Page<E> page, List<T> content) {
        long from = content.isEmpty() ? 0 : page.getPageable().getOffset() + 1;
        long to = content.isEmpty() ? 0 : from + content.size() - 1;
        List<String> sort = page.getSort().stream()
                .map(o -> o.getProperty() + "," + o.getDirection().name().toLowerCase())
                .toList();
        return new PageResponse<>(content, page.getNumber() + 1, page.getSize(), page.getTotalElements(),
                page.getTotalPages(), from, to, sort);
    }
}
