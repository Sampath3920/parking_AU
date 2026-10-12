package com.smartparking.backend.dto;

import java.util.Collections;
import java.util.List;

/** One page of a list, plus the numbers a table needs to draw its page buttons. */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static final int MAX_SIZE = 100;

    /** Cuts one page out of a full (already filtered and sorted) list. page starts at 0. */
    public static <T> PageResult<T> of(List<T> all, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        int safePage = Math.max(page, 0);
        int total = all.size();
        int totalPages = (int) Math.ceil(total / (double) safeSize);
        int from = (int) Math.min((long) safePage * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<T> slice = from >= to ? Collections.emptyList() : List.copyOf(all.subList(from, to));
        return new PageResult<>(slice, safePage, safeSize, total, totalPages);
    }
}
