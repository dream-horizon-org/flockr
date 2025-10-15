package com.ascend.flockr.common.utils;

import com.ascend.flockr.common.constants.Constants;
import com.ascend.flockr.common.exception.errors.DefinedErrors;
import com.dream11.rest.util.ExceptionUtil;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonUtils {
  @Getter
  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern(Constants.DATE_PATTERN);

  private CommonUtils() {
    throw new UnsupportedOperationException("Constructor Invocation Unavailable for CommonUtils");
  }

  public static Integer getNumOfCores() {
    return CpuCoreSensor.availableProcessors();
  }

  public static String getUserKey(Long userId, String guestId) {
    return Objects.nonNull(userId) ? userId.toString() : guestId;
  }

  public static String getAerospikeSetNameFromSource(String baseSetName, String source) {
    if (Objects.isNull(source) || source.equals(Constants.SOURCE_DREAM11)) return baseSetName;
    else return baseSetName + "-" + source.toLowerCase();
  }

  public static long getEpochFromExpireAt(String expireAt, String action) {
    long expiryEpoch =
        LocalDateTime.parse(expireAt, formatter).toEpochSecond(ZoneOffset.UTC) * 1000L;
    long currentTime = System.currentTimeMillis();
    if (expiryEpoch < currentTime) {
      if (action.equals(Constants.ACTION_APPEND)) {
        log.error("Invalid expiryAt: {}", expireAt);
        throw ExceptionUtil.getException(DefinedErrors.INVALID_EXPIRY_TIME, expireAt);
      } else return 0L;
    }
    return expiryEpoch;
  }
}
