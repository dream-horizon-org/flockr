package com.ascend.flockr.util;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Row;
import java.util.Map;

public class JsonColumnUtil {

  public static JsonObject set(Object obj) {
    return JsonObject.mapFrom(obj);
  }

  public static <T> T get(Row row, String columnName, Class<T> type) {
    JsonObject jsonVal = row.get(JsonObject.class, row.getColumnIndex(columnName));
    if (jsonVal != null) {
      try {
        return jsonVal.mapTo(type);
      } catch (Exception ex) {
        //                throw new DefinedException(ErrorEntity.PARSING_FAILED, ex.getMessage());
        throw new RuntimeException(ex);
      }
    }

    return null;
  }

  public static Map<String, Object> get(Row row, String columnName) {
    JsonObject jsonVal = row.get(JsonObject.class, row.getColumnIndex(columnName));
    if (jsonVal != null) {
      try {
        return jsonVal.getMap();
      } catch (Exception ex) {
        //                throw new DefinedException(ErrorEntity.PARSING_FAILED, ex.getMessage());
        throw new RuntimeException(ex);
      }
    }

    return null;
  }
}
