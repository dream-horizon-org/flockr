package com.ascend.flockr.io.response;

import lombok.Data;

@Data
public class Pagination {

  private Integer pageSize;
  private Integer pageNum;
  private Integer pageCount;
  private Integer recordCount;

  public Pagination(Integer pageSize, Integer pageNum, Integer recordCount) {
    this.pageSize = pageSize;
    this.pageNum = pageNum;
    this.pageCount =
        (recordCount % pageSize) > 0 ? (recordCount / pageSize) + 1 : recordCount / pageSize;
    this.recordCount = recordCount;
  }
}
