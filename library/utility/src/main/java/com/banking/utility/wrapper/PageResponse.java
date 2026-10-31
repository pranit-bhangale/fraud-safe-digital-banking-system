package com.banking.utility.wrapper;

import lombok.Builder;
import org.springframework.data.domain.Page;
import org.springframework.util.Assert;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a paginated response containing mapped content and pagination metadata.
 *
 * @param contents      the content of the current page
 * @param currentPage   the zero-based index of the current page
 * @param pageSize      the number of elements per page
 * @param totalElements the total number of elements across all pages
 * @param totalPages    the total number of pages
 * @param isLastPage    whether the current page is the last page
 * @param isFirstPage   whether the current page is the first page
 * @param <T>           the type of content elements
 */
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

    /**
     * Compact constructor validating and ensuring immutability of page contents.
     *
     * @param contents      the content of the current page
     * @param currentPage   the zero-based index of the current page
     * @param pageSize      the number of elements per page
     * @param totalElements the total number of elements across all pages
     * @param totalPages    the total number of pages
     * @param isLastPage    whether the current page is the last page
     * @param isFirstPage   whether the current page is the first page
     */
    public PageResponse {
        contents = contents == null ? List.of() : List.copyOf(contents);
        Assert.isTrue(currentPage >= 0, "Current page must be >= 0");
        Assert.isTrue(pageSize >= 0, "Page size must be >= 0");
        Assert.isTrue(totalElements >= 0, "Total elements must be >= 0");
        Assert.isTrue(totalPages >= 0, "Total pages must be >= 0");
    }

    /**
     * Creates a {@link PageResponse} directly from a Spring Data {@link Page} without transformation.
     *
     * @param page the Spring Data page to convert
     * @param <T>  the item type
     * @return a new {@link PageResponse} instance
     */
    public static <T> PageResponse<T> from(final Page<T> page) {
        return from(page, Function.identity());
    }

    /**
     * Creates a {@link PageResponse} by transforming each element of a Spring Data {@link Page}
     * using the provided mapping function.
     *
     * @param page   the Spring Data page to convert
     * @param mapper the mapping function to convert elements from type {@code S} to type {@code T}
     * @param <S>    the source element type
     * @param <T>    the target element type
     * @return a new {@link PageResponse} containing the mapped items
     */
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
