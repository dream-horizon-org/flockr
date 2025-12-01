package com.dream11.flocker.engine.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@JsonDeserialize(using = ConnectorConfigDeserializer.class)
public class ConnectorConfig {
    private String type;
    private Object config;

    public ConnectorConfig(String type, Object config) {
        this.type = type;
        this.config = config;
    }
}
