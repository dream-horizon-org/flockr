package com.ascend.flockr.exception;

import com.dream11.rest.exception.RestException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefinedException extends RestException {

  public DefinedException(ErrorEntity errorEntity) {
    super(
        errorEntity.getCause(),
        String.valueOf(errorEntity.getError()),
        errorEntity.getHttpStatusCode());
  }

  public DefinedException(ErrorEntity errorEntity, Object... args) {
    super(
        String.format(errorEntity.getCause(), args),
        String.valueOf(errorEntity.getError()),
        errorEntity.getHttpStatusCode());
  }
}
