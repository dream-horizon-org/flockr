package io.ascend.flockr.admin.client.webclient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.web.client.HttpRequest;
import io.vertx.rxjava3.ext.web.client.HttpResponse;

/**
 * Client interface for making HTTP requests using Vert.x Web Client.
 *
 * <p>This client provides methods for:
 *
 * <ul>
 *   <li>Preparing HTTP requests (GET, POST, PUT)
 *   <li>Executing HTTP requests with optional request bodies
 *   <li>Configuring circuit breaker for resilience
 * </ul>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public interface WebClient {

  /**
   * Closes the web client and releases all associated resources.
   *
   * @return a Completable that completes when the client is closed
   */
  Completable close();

  /**
   * Sets a circuit breaker for the web client to provide resilience.
   *
   * @param circuitBreaker the circuit breaker instance to use
   * @return this WebClient instance for method chaining
   */
  @Fluent
  WebClient setCircuitBreaker(CircuitBreaker circuitBreaker);

  /**
   * Prepares an HTTP GET request.
   *
   * @param host the target host
   * @param port the target port
   * @param apiEndPoint the API endpoint path
   * @return an HttpRequest that can be further configured and executed
   */
  HttpRequest<Buffer> prepareHttpGETRequest(String host, Integer port, String apiEndPoint);

  /**
   * Prepares an HTTP PUT request.
   *
   * @param host the target host
   * @param port the target port
   * @param apiEndPoint the API endpoint path
   * @return an HttpRequest that can be further configured and executed
   */
  HttpRequest<Buffer> prepareHttpPUTRequest(String host, Integer port, String apiEndPoint);

  /**
   * Prepares an HTTP POST request.
   *
   * @param host the target host
   * @param port the target port
   * @param apiEndPoint the API endpoint path
   * @return an HttpRequest that can be further configured and executed
   */
  HttpRequest<Buffer> prepareHttpPOSTRequest(String host, Integer port, String apiEndPoint);

  /**
   * Prepares an HTTP POST request with an absolute URL.
   *
   * @param host the absolute URL including protocol, host, port, and path
   * @return an HttpRequest that can be further configured and executed
   */
  HttpRequest<Buffer> prepareHttpPostAbsRequest(String host);

  /**
   * Executes an HTTP request without a request body.
   *
   * @param request the prepared HTTP request
   * @return a Single emitting the HTTP response
   */
  Single<HttpResponse<Buffer>> execute(HttpRequest<Buffer> request);

  /**
   * Executes an HTTP request with a JSON request body.
   *
   * @param request the prepared HTTP request
   * @param jsonObject the JSON object to send as the request body
   * @return a Single emitting the HTTP response
   */
  Single<HttpResponse<Buffer>> execute(HttpRequest<Buffer> request, JsonObject jsonObject);
}
