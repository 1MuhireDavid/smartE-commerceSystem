package org.example.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated payload included inside {@link ApiResponse#getData()}.
 */
@Schema(description = "Paginated result set")
public class PagedResponse<T> {

    @Schema(description = "Items on the current page")
    private final List<T> content;

    @Schema(description = "Zero-based current page index", example = "0")
    private final int page;

    @Schema(description = "Requested page size", example = "20")
    private final int size;

    @Schema(description = "Total number of matching items across all pages", example = "154")
    private final long totalElements;

    @Schema(description = "Total number of pages", example = "8")
    private final int totalPages;

    @Schema(description = "Whether this is the last page", example = "false")
    private final boolean last;

    private PagedResponse(Page<T> page) {
        this.content       = page.getContent();
        this.page          = page.getNumber();
        this.size          = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages    = page.getTotalPages();
        this.last          = page.isLast();
    }

    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(page);
    }

    public List<T> getContent()       { return content; }
    public int     getPage()          { return page; }
    public int     getSize()          { return size; }
    public long    getTotalElements() { return totalElements; }
    public int     getTotalPages()    { return totalPages; }
    public boolean isLast()           { return last; }
}
