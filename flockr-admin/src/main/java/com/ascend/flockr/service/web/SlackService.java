package com.ascend.flockr.service.web;

import io.reactivex.Completable;

public interface SlackService {

  Completable postMessageColorCode(String email, String message, Long cohortId, String colorCode);

  Completable postMessageRed(String email, String message, Long cohortId);

  Completable postMessageGreen(String email, String message, Long cohortId);

  Completable postMessageWithHeaderColorCode(
      String email, String message, Long cohortId, String header, String colorCode);

  Completable postMessageWithHeaderRed(String email, String message, Long cohortId, String header);

  Completable postMessageWithHeaderGreen(
      String email, String message, Long cohortId, String header);
}
