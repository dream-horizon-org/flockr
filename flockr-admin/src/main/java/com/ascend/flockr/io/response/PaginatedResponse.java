package com.ascend.flockr.io.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

  private List<T> records;
  private Pagination pagination;

  public PaginatedResponse(List<T> records) {
    this.records = records;
  }
}
