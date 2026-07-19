package com.argent.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int pageSize;
    private long total;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> PagedResponse<T> of(List<T> content, int page, int pageSize, long total) {
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return PagedResponse.<T>builder()
                .content(content)
                .page(page)
                .pageSize(pageSize)
                .total(total)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }

    public ApiResponse.Meta toMeta() {
        return ApiResponse.Meta.builder()
                .page(page + 1)
                .pageSize(pageSize)
                .total(total)
                .totalPages(totalPages)
                .build();
    }
}
