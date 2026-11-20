package com.banking.user.wrapper;

import lombok.Builder;
import org.springframework.data.domain.Page;
import org.springframework.util.Assert;

import java.util.List;
import java.util.function.Function;

@Builder
public record PageResponse<T>(
        List<T> contents,
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean isLastPage,
        boolean isFirstPage
) {
    
    public static <T> PageResponse<T> from(final Page<T> page) {
        return from(page, Function.identity());
    }

    public static <S, T> PageResponse<T> from(
            final Page<S> page, final Function<? super S, ? extends T> mapper
    ) {
        Assert.notNull(page, "Page must not be null");
        Assert.notNull(mapper, "Mapper must not be null");
        final List<T> contents = page.getContent()
                .stream()
                .<T>map(mapper)
                .toList();
        return new PageResponse<>(
                contents,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }
}
