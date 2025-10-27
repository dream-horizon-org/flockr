package com.ascend.flockr.exception;

import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import lombok.Getter;

@Getter
public class RemoteServiceError extends Exception {

  private final HttpResponse<Buffer> response;

  private static final String ERROR_MESSAGE_TEMPLATE =
      "responseCode: %s, responseMessage: %s, responseBody: %s";

  public RemoteServiceError(HttpResponse<Buffer> httpResponse) {
    super(
        String.format(
            ERROR_MESSAGE_TEMPLATE,
            httpResponse.statusCode(),
            httpResponse.statusMessage(),
            httpResponse.bodyAsString()));
    this.response = httpResponse;
  }
}
