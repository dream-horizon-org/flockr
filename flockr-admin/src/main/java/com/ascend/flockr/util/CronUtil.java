package com.ascend.flockr.util;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;

public class CronUtil {

  private static final CronParser cronParser =
      new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

  public static Long nextExecutionTime(String unixCronExpression, Long fromTimeInSec) {
    ZonedDateTime fromZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(fromTimeInSec), ZoneOffset.UTC);
    ExecutionTime executionTime = ExecutionTime.forCron(cronParser.parse(unixCronExpression));
    return executionTime.nextExecution(fromZonedDateTime).get().toEpochSecond();
  }

  public static Long nextExecutionTimeByZoneId(
      String unixCronExpression, Long fromTimeInSec, CronTimeZone zoneId) {
    ZonedDateTime fromZonedDateTime =
        ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(fromTimeInSec), ZoneId.of(zoneId.name(), ZoneId.SHORT_IDS));
    ExecutionTime executionTime = ExecutionTime.forCron(cronParser.parse(unixCronExpression));
    return executionTime.nextExecution(fromZonedDateTime).get().toEpochSecond();
  }

  public static Boolean isMinimumHourlyCronDifference(String cronExpression) {
    return Arrays.asList(cronExpression.split(" ")).subList(0, 1).stream()
        .anyMatch(
            unit -> {
              try {
                Long.parseLong(unit);
              } catch (NumberFormatException e) {
                return false;
              }
              return true;
            });
  }
}
