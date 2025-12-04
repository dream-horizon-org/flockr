package io.ascend.flockr.admin.client.webclient.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.webclient.WebClient;
import io.ascend.flockr.admin.config.WebClientConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.netty.handler.timeout.TimeoutException;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Consumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.web.client.HttpRequest;
import io.vertx.rxjava3.ext.web.client.HttpResponse;
import java.net.ConnectException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebClientImpl implements WebClient {

  private final io.vertx.rxjava3.ext.web.client.WebClient webClient;
  private CircuitBreaker circuitBreaker;

  @Inject
  public WebClientImpl(Vertx vertx, WebClientConfig webClientConfig) {
    this.webClient =
        io.vertx.rxjava3.ext.web.client.WebClient.create(
            vertx, getWebClientOptions(webClientConfig));
  }

  @Override
  public HttpRequest<Buffer> prepareHttpGETRequest(String host, Integer port, String apiEndPoint) {
    return webClient.get(port, host, apiEndPoint).timeout(3000);
    //            .expect(validateHttpResponse());
  }

  @Override
  public HttpRequest<Buffer> prepareHttpPUTRequest(String host, Integer port, String apiEndPoint) {
    return webClient.put(port, host, apiEndPoint).timeout(3000);
    //        .expect(validateHttpResponse());
  }

  @Override
  public HttpRequest<Buffer> prepareHttpPOSTRequest(String host, Integer port, String apiEndPoint) {
    return webClient.post(port, host, apiEndPoint).timeout(3000);
    //        .expect(validateHttpResponse());
  }

  @Override
  public HttpRequest<Buffer> prepareHttpPostAbsRequest(String absUri) {
    return webClient.postAbs(absUri).timeout(3000);
  }

  @Override
  public HttpRequest<Buffer> prepareHttpPutAbsRequest(String absUri) {
    return webClient.putAbs(absUri).timeout(3000);
  }

  @Override
  public HttpRequest<Buffer> prepareHttpPatchAbsRequest(String absUri) {
    return webClient.patchAbs(absUri).timeout(3000);
  }

  @Override
  public Single<HttpResponse<Buffer>> execute(HttpRequest<Buffer> request, JsonObject jsonObject) {
    return request
        .rxSendJson(jsonObject)
        .retry(1, this::retryable)
        .doOnSuccess(httpSuccessResponseConsumer(request))
        .doOnError(httpErrorResponseConsumer(request))
        .map(httpResponse -> httpResponse);
  }

  @Override
  public Single<HttpResponse<Buffer>> execute(HttpRequest<Buffer> request) {
    return request
        .rxSend()
        .retry(2, this::retryable)
        .doOnSuccess(httpSuccessResponseConsumer(request))
        .doOnError(httpErrorResponseConsumer(request))
        .map(httpResponse -> httpResponse);
  }

  @Override
  public Single<HttpResponse<Buffer>> execute(HttpRequest<Buffer> request, Object body) {
    return request
        .rxSendJson(body)
        .retry(1, this::retryable)
        .doOnSuccess(httpSuccessResponseConsumer(request))
        .doOnError(httpErrorResponseConsumer(request))
        .map(httpResponse -> httpResponse);
  }

  @Override
  public Completable close() {
    return Completable.fromAction(webClient::close);
  }

  @Override
  public WebClient setCircuitBreaker(CircuitBreaker circuitBreaker) {
    this.circuitBreaker = circuitBreaker;
    return this;
  }

  private static WebClientOptions getWebClientOptions(WebClientConfig webClientConfig) {
    return new WebClientOptions()
        .setPipeliningLimit(webClientConfig.getPipeliningLimit())
        .setConnectTimeout(webClientConfig.getConnectTimeout())
        .setMaxPoolSize(webClientConfig.getMaxPoolSize())
        .setLogActivity(webClientConfig.isLogActivity())
        .setKeepAlive(webClientConfig.isKeepAlive())
        .setKeepAliveTimeout(webClientConfig.getKeepAliveTimeout())
        .setPipelining(webClientConfig.isPipelining());
  }

  private void printCircuitBreakerState() {

    if (Objects.isNull(circuitBreaker)) return;
    log.info(
        "CircuitBreaker:{} | Successful call count:{} | Failed call count:{} | Failure rate:{}% | State:{}",
        circuitBreaker.getName(),
        circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(),
        circuitBreaker.getMetrics().getNumberOfFailedCalls(),
        circuitBreaker.getMetrics().getFailureRate(),
        circuitBreaker.getState());
  }

  private Consumer<Throwable> httpErrorResponseConsumer(HttpRequest<Buffer> request) {
    return throwable -> {
      log.error(
          "Error while making request to resource: {}", request.host() + request.uri(), throwable);
      printCircuitBreakerState();
    };
  }

  private Consumer<HttpResponse<Buffer>> httpSuccessResponseConsumer(HttpRequest<Buffer> request) {
    return bufferHttpResponse -> {
      log.info(
          "Response of API - {} : {}",
          request.host() + request.uri(),
          bufferHttpResponse.bodyAsString());
      printCircuitBreakerState();
    };
  }

  private boolean retryable(Throwable exception) {
    if (exception instanceof ConnectException || exception instanceof TimeoutException) {
      log.error("error occurred while attempting to connect a socket, retrying http request...");
      return true;
    } else {
      return false;
    }
  }
}
