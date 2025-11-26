package io.ascend.flockr.admin.client.webclient.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.webclient.WebClient;
import io.ascend.flockr.admin.config.WebClientConfig;
import io.ascend.flockr.admin.constants.web.WebConstants;
import io.ascend.flockr.admin.util.CommonUtil;
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
import io.vertx.rxjava3.ext.web.client.predicate.ResponsePredicateResult;
import java.net.ConnectException;
import java.util.Objects;
import java.util.function.Function;
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
    //        .expect(validateHttpResponse());
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

  private void pushCircuitBreakerMetricsToDD() {
    if (Objects.isNull(circuitBreaker)) return;

    String tag = CommonUtil.getCircuitBreakerTag(circuitBreaker.getName());
    CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

    pushGaugeMetricToDD(WebConstants.STATE, circuitBreaker.getState().getOrder(), tag);
    pushGaugeMetricToDD(WebConstants.BUFFERED_CALLS_COUNT, metrics.getNumberOfBufferedCalls(), tag);
    pushGaugeMetricToDD(
        WebConstants.NOT_PERMITTED_CALLS_COUNT, metrics.getNumberOfNotPermittedCalls(), tag);
    pushGaugeMetricToDD(
        WebConstants.SUCCESSFUL_CALLS_COUNT, metrics.getNumberOfSuccessfulCalls(), tag);
    pushGaugeMetricToDD(WebConstants.FAILED_CALLS_COUNT, metrics.getNumberOfFailedCalls(), tag);
    pushGaugeMetricToDD(WebConstants.SLOW_CALLS_COUNT, metrics.getNumberOfSlowCalls(), tag);
    pushGaugeMetricToDD(
        WebConstants.SLOW_SUCCESSFUL_CALLS_COUNT, metrics.getNumberOfSlowSuccessfulCalls(), tag);
    pushGaugeMetricToDD(
        WebConstants.SLOW_FAILED_CALLS_COUNT, metrics.getNumberOfSlowFailedCalls(), tag);
    pushGaugeMetricToDD(WebConstants.SLOW_CALL_RATE, metrics.getSlowCallRate(), tag);
    pushGaugeMetricToDD(WebConstants.FAILURE_RATE, metrics.getFailureRate(), tag);
  }

  private <T extends Number> void pushGaugeMetricToDD(
      String aspectName, T metricValue, String... tags) {}

  private Function<HttpResponse<Void>, ResponsePredicateResult> validateHttpResponse() {
    return httpResponse -> {
      if (httpResponse.statusCode() < 200 || httpResponse.statusCode() > 299)
        return ResponsePredicateResult.failure(
            "HTTP Error from Remote Service "
                + "Status Code: "
                + httpResponse.statusCode()
                + " Status Message: "
                + httpResponse.statusMessage());
      return ResponsePredicateResult.success();
    };
  }

  private Consumer<Throwable> httpErrorResponseConsumer(HttpRequest<Buffer> request) {
    return throwable -> {
      log.error(
          "Error while making request to resource: {}", request.host() + request.uri(), throwable);
      printCircuitBreakerState();
      pushCircuitBreakerMetricsToDD();
    };
  }

  private Consumer<HttpResponse<Buffer>> httpSuccessResponseConsumer(HttpRequest<Buffer> request) {
    return bufferHttpResponse -> {
      log.info(
          "Response of API - {} : {}",
          request.host() + request.uri(),
          bufferHttpResponse.bodyAsString());
      printCircuitBreakerState();
      pushCircuitBreakerMetricsToDD();
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
