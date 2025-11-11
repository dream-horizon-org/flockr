package com.ascend.flockr.io.response;

import java.util.List;

public record PaginatedResponse<T>(PageInfo pageInfo, List<T> data) {

  public record PageInfo(int page, int pageSize, boolean hasMore) {}
}
