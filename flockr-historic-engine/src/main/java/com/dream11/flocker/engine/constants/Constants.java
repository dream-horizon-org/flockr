package com.dream11.flocker.engine.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final String APP_ENV_KEY = "app.env";
    public static final String DEFAULT_APP_ENV = "default";
    public static final String SOURCE_TYPE_KEY = "source.type";
    public static final String SINK_TYPE_KEY = "sink.type";
    public static final String SPARK_MASTER = "spark.master";
    public static final String COHORT_ID_KEY = "cohort.id";
    public static final String ACTION_KEY = "action";
    
    public static final String ACTION_APPEND = "append";
    public static final String ACTION_REMOVE = "remove";
    
    public static final String WRITE_MODE_APPEND = "append";
    public static final String WRITE_MODE_OVERWRITE = "overwrite";
    
    public static final String OUTPUT_PATH_PREFIX = "output/";

}
