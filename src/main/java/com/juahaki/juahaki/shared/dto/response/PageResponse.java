package com.juahaki.juahaki.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Generic paginated response wrapper that can be used across all features.
 * Provides clean pagination metadata without Spring's Page overhead.
 *
 * @param <T> the type of content being paginated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    /**
     * Create PageResponse from Spring's Page object
     *
     * @param page the Spring Page object
     * @param <T>  the content type
     * @return PageResponse with clean pagination data
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Create PageResponse from Spring's Page object with content transformation
     *
     * @param page   the Spring Page object
     * @param mapper function to transform content from source type to target type
     * @param <S>    source content type
     * @param <T>    target content type
     * @return PageResponse with transformed content and clean pagination data
     */
    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        List<T> transformedContent = page.getContent().stream()
                .map(mapper)
                .toList();

        return PageResponse.<T>builder()
                .content(transformedContent)
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Create PageResponse from Spring's Page object with complex content transformation
     * Useful when transformation requires additional context or multiple operations
     *
     * @param page            the Spring Page object
     * @param transformedContent the already transformed content list
     * @param <S>             source content type
     * @param <T>             target content type
     * @return PageResponse with transformed content and clean pagination data
     */
    public static <S, T> PageResponse<T> of(Page<S> page, List<T> transformedContent) {
        return PageResponse.<T>builder()
                .content(transformedContent)
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Check if the page is empty
     *
     * @return true if no content exists
     */
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    /**
     * Get the number of elements in current page
     *
     * @return size of current page content
     */
    public int getNumberOfElements() {
        return content != null ? content.size() : 0;
    }

    /**
     * Check if this is the first page
     *
     * @return true if current page is 0
     */
    public boolean isFirst() {
        return currentPage == 0;
    }

    /**
     * Check if this is the last page
     *
     * @return true if this is the last page
     */
    public boolean isLast() {
        return currentPage == totalPages - 1;
    }
}
