package com.ascend.flockr.util;

import com.ascend.flockr.exception.DefinedException;
import com.ascend.flockr.exception.ErrorEntity;
import com.ascend.flockr.io.DynamicExpireTimeUnit;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {

  private static final DateFormat defaultDateFormat = new SimpleDateFormat("MM-dd-yyyy");
  private static final DateFormat defaultDateTimeFormat =
      new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
  private static final DateFormat utcDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  static {
    defaultDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
    defaultDateTimeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
    utcDateTimeFormat.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
  }

  /**
   * @return epoch seconds
   */
  public static Long parse(String date) throws ParseException {
    return defaultDateFormat.parse(date).getTime() / 1000;
  }

  public static String toDateString(Long epochSeconds) {
    Date date = new Date(epochSeconds * 1000);
    return defaultDateFormat.format(date);
  }

  public static String toDateTimeString(Long epochSeconds) {
    Date dateTime = new Date(epochSeconds * 1000);
    return defaultDateTimeFormat.format(dateTime);
  }

  public static String toDateTimeStringUtc(Long epochSeconds) {
    Date dateTime = new Date(epochSeconds * 1000);
    return utcDateTimeFormat.format(dateTime);
  }

  public static Long toEpochMilli(LocalDateTime dateTime, ZoneOffset zoneId) {
    return dateTime.atZone(zoneId).toInstant().toEpochMilli();
  }

  public static Long valueInSeconds(DynamicExpireTimeUnit unit) {
    switch (unit) {
      case day:
        return (long) 24 * 60 * 60;
      case hour:
        return (long) 60 * 60;
      default:
        throw new DefinedException(ErrorEntity.DYNAMIC_EXPIRE_UNIT_NOT_SUPPORTED, unit);
    }
  }
}
