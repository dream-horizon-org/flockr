package com.ascend.flockr.io.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {
  private boolean success;
  private int status;
  private String message;
  private T data;

  public static <T> Response<T> successfulResponse(T data, int httpStatusCode) {
    return new Response<>(true, httpStatusCode, "success", data);
  }

  public static <T> Response<T> errorResponse(String message, int httpStatusCode) {
    return new Response<>(false, httpStatusCode, message, null);
  }
}
