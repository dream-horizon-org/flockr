package com.ascend.flockr.util;

import com.ascend.flockr.io.response.Response;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class RestResponseUtil {

    private RestResponseUtil() {
    }

    /** Converts a RxJava Single<Response<T>> to a CompletionStage<Response<T>> */
    private static <T> CompletionStage<Response<T>> fromSingle(Single<Response<T>> single) {
        CompletableFuture<Response<T>> future = new CompletableFuture<>();
        single.subscribe(future::complete, future::completeExceptionally);
        return future;
    }

    /**
     * Converts a Single<T> (normal data) into CompletionStage<Response<T>>
     * wrapping success or error into your Response model.
     */
    public static <T> Function<Single<T>, CompletionStage<Response<T>>> jaxrsRestHandler() {
        return single ->
                fromSingle(
                        single
                                .map(value -> Response.successfulResponse(value, 200))
                                .onErrorReturn(error ->
                                        Response.errorResponse(error.getMessage(), 500))
                );
    }
}
