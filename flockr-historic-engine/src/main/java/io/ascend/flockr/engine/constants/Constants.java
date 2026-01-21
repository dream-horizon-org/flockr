package io.ascend.flockr.engine.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public static final String KEY_APP_ENV = "app.env";
  public static final String DEFAULT_APP_ENV = "default";
  public static final String KEY_SOURCE_TYPE = "source.type";
  public static final String KEY_SINK_TYPE = "sink.type";
  public static final String KEY_SPARK_MASTER = "spark.master";
  public static final String KEY_ACTION = "action";

  public static final String ACTION_APPEND = "append";
  public static final String ACTION_REMOVE = "remove";

  public static final String WRITE_MODE_APPEND = "append";
  public static final String WRITE_MODE_OVERWRITE = "overwrite";

  public static final String OUTPUT_PATH_PREFIX = "output/";
  public static final String FLOCKR_SPARK_MASTER = "flockr-spark-master";
  public static final String FLOCKR_SPARK_MASTER_PORT = "7077";

  public static final String USER_ID_COLUMN = "user_id";
  public static final String AUDIENCE_NAME_COLUMN = "audience_name";
  public static final String ACTION_COLUMN = "action";
  public static final String EXPIRE_AT_COLUMN = "expire_at";
}
