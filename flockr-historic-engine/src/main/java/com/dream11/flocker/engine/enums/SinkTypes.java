package com.dream11.flocker.engine.enums;

public enum SinkTypes {
    KAFKA("kafka"),
    S3("s3"),
    API("api");

    private final String configType;

    SinkTypes(String configType) {
        this.configType = configType;
    }

    public String getConfigType() {
        return configType;
    }
}
