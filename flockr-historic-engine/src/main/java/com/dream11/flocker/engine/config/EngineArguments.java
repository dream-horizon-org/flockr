package com.dream11.flocker.engine.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineArguments {

    @JsonProperty("sqlQuery")
    private String sqlQuery;

    @JsonProperty("cohortId")
    private String cohortId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("sparkMaster")
    private String sparkMaster;

    @JsonProperty("sourceJson")
    private List<ConnectorConfig> sourceJson;

    @JsonProperty("destinationJson")
    private List<ConnectorConfig> destinationJson;
}

