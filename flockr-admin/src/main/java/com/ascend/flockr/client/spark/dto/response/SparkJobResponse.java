package com.ascend.flockr.client.spark.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic Spark job response (deprecated - use SparkJobSubmissionResponse or SparkJobStatusResponse).
 * 
 * @deprecated Use SparkJobSubmissionResponse or SparkJobStatusResponse instead
 */
@Deprecated
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkJobResponse {
    private String sparkApplicationId;
    private String status;
    private String message;
}
