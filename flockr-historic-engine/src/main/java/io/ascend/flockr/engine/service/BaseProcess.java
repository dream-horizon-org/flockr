package io.ascend.flockr.engine.service;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.ascend.flockr.engine.dto.AudienceMetadata;
import io.ascend.flockr.engine.dto.UserIdRow;
import io.ascend.flockr.engine.exception.JobException;
import io.ascend.flockr.engine.modules.sink.Sink;
import io.ascend.flockr.engine.modules.source.Source;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;

/**
 * Base class for data processing services in the Flocker Historic Engine.
 *
 * <p>This class provides orchestration for processing data from sources and writing to sinks. It
 * handles:
 *
 * <ul>
 *   <li><b>Source:</b> Handles query execution, validation, and user ID extraction
 *   <li><b>BaseProcess:</b> Orchestrates the flow and manages caching
 *   <li><b>Sinks:</b> Handle data delivery to destinations (Kafka, API, S3)
 * </ul>
 *
 * @see Source
 * @see Sink
 * @see AudienceMetadata
 * @see UserIdRow
 * @author Shivam-Raghuwanshi
 */
@Slf4j
@Data
public class BaseProcess {

  /** Spark session for distributed data processing. */
  protected final SparkSession sparkSession;

  /** Single configured data source. */
  protected final Source source;

  /** List of configured data sinks. */
  protected final List<Sink> sinks;

  @Inject
  public BaseProcess(
      SparkSession sparkSession, @Named("source") Source source, @Named("sinks") List<Sink> sinks) {
    this.sparkSession = sparkSession;
    this.source = source;
    this.sinks = sinks;
  }

  public void processWithQuery(
      String xProjectId, String audienceName, String action, Long expireAt) {
    if (source == null) {
      throw new IllegalStateException("Source is not configured");
    }
    try {
      log.info("Starting processing for audience: {}, action: {}", audienceName, action);
      Dataset<UserIdRow> userIds = source.read();
      userIds = userIds.cache();
      long count = userIds.count();
      log.info("Processing {} user IDs for audience: {}", count, audienceName);
      AudienceMetadata metadata =
          AudienceMetadata.builder()
              .audienceName(audienceName)
              .action(action)
              .expireAt(expireAt)
              .xProjectId(xProjectId)
              .build();
      writeToSinks(userIds, metadata);
    } catch (Exception e) {
      log.error("Error processing data", e);
      throw new JobException("Error occurred in execution");
    }
  }

  private void writeToSinks(Dataset<UserIdRow> userIds, AudienceMetadata metadata) {
    log.info("Writing to {} sinks...", sinks.size());
    for (Sink sink : sinks) {
      sink.write(userIds, metadata);
    }
  }
}
