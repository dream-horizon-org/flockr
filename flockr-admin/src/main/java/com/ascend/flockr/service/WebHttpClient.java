package com.ascend.flockr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import java.util.Map;

public interface WebHttpClient {

  <T> Single<T> get(Long timeout, String host, Integer port, String uri, Class<T> typeClazz);

  <T> Single<T> get(
      Long timeout,
      String host,
      Integer port,
      String uri,
      String bearerToken,
      MultiMap header,
      Class<T> typeClazz);

  Single<JsonObject> getByAbsoluteUri(
      Long timeout,
      String absUri,
      String bearerToken,
      Map<String, String> queryParam,
      MultiMap header);

  <T> Single<T> get(Long timeout, String host, Integer port, String uri, TypeReference<T> typeRef);

  Single<Buffer> getBuffer(Long timeout, String host, Integer port, String uri);

  <T> Single<T> get(
      Long timeout,
      String host,
      Integer port,
      String uri,
      Map<String, String> queryParam,
      Class<T> typeClazz);

  <T> Single<T> post(
      Long timeout,
      String host,
      Integer port,
      String uri,
      Map<String, String> queryParam,
      Class<T> typeClazz);

  <T, U> Single<T> post(
      Long timeout, String host, Integer port, String uri, U requestBody, Class<T> typeClazz);

  <U> Single<JsonObject> postByAbsoluteUri(
      Long timeout,
      String absUri,
      U requestBody,
      String bearerToken,
      Map<String, String> queryParam,
      MultiMap header);

  Completable patch(Long timeout, String host, Integer port, String uri, Integer httpSuccessCode);

  <T, U> Single<T> patch(
      Long timeout,
      String host,
      Integer port,
      String uri,
      String bearerToken,
      U requestBody,
      Class<T> typeClazz);

  <T, U> Single<T> post(
      Long timeout,
      String host,
      Integer port,
      String uri,
      String bearerToken,
      U requestBody,
      Class<T> typeClazz);

  <U> Completable put(Long timeout, String host, Integer port, String uri, U requestBody);
}
