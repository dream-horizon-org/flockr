package io.ascend.flockr.admin.io.response;

import java.util.List;

public record PaginatedResponse<T>(PageInfo pageInfo, List<T> data) {

  public record PageInfo(int page, int pageSize, boolean hasMore) {}
}
