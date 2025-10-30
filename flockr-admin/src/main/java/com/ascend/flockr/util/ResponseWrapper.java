package com.ascend.flockr.util;

import com.ascend.flockr.io.response.Response;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import java.util.concurrent.CompletableFuture;

public class ResponseWrapper {

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static <T> CompletableFuture<Response<T>> fromMaybe(
      Maybe<T> source, T defaultValue, int httpStatusCode) {
    CompletableFuture<Response<T>> future = new CompletableFuture<>();
    source.subscribe(
        value -> future.complete(Response.successfulResponse(value, httpStatusCode)),
        future::completeExceptionally,
        () -> future.complete(Response.successfulResponse(defaultValue, httpStatusCode)));
    return future;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static <T> CompletableFuture<Response<T>> fromCompletable(
      Completable source, T value, int httpStatusCode) {
    CompletableFuture<Response<T>> future = new CompletableFuture<>();
    source.subscribe(
        () -> future.complete(Response.successfulResponse(value, httpStatusCode)),
        future::completeExceptionally);
    return future;
  }
}
