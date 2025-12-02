package io.ascend.flockr.admin.io.response;

import java.util.List;

/**
 * Response wrapper for paginated API results.
 *
 * <p>This record provides a standardized structure for paginated data, including metadata about the
 * current page and whether more data is available.
 *
 * @param pageInfo metadata about the current page
 * @param data the list of items for the current page
 * @param <T> the type of items in the paginated list
 * @author Prithu Sharma
 * @since 1.0
 */
public record PaginatedResponse<T>(PageInfo pageInfo, List<T> data) {

  /**
   * Metadata about the current page in a paginated response.
   *
   * @param page the current page number (0-indexed)
   * @param pageSize the number of items per page
   * @param hasMore true if more pages are available, false otherwise
   */
  public record PageInfo(int page, int pageSize, boolean hasMore) {}
}
