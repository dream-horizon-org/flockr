package com.ascend.flockr.client.webclient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.web.client.HttpRequest;
import io.vertx.rxjava3.ext.web.client.HttpResponse;

public interface WebClient {

  Completable close();

  @Fluent
  WebClient setCircuitBreaker(CircuitBreaker circuitBreaker);

  HttpRequest<Buffer> prepareHttpGETRequest(String host, Integer port, String apiEndPoint);

  HttpRequest<Buffer> prepareHttpPUTRequest(String host, Integer port, String apiEndPoint);

  HttpRequest<Buffer> prepareHttpPOSTRequest(String host, Integer port, String apiEndPoint);

  HttpRequest<Buffer> prepareHttpPostAbsRequest(String host);

  Single<HttpResponse<Buffer>> execute(HttpRequest<Buffer> request);

  Single<HttpResponse<Buffer>> execute(HttpRequest<Buffer> request, JsonObject jsonObject);
}
