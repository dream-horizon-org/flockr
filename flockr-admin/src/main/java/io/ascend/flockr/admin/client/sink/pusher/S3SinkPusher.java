package io.ascend.flockr.admin.client.sink.pusher;

import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.audience.AudienceRecord;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.config.S3FolderSinkConfig;
import io.ascend.flockr.admin.util.ConfigurationUtil;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Sink pusher implementation for AWS S3.
 *
 * <p>Uploads audience records as files to the configured S3 bucket and folder. Files are named with
 * audience ID and timestamp for uniqueness.
 */
@Slf4j
public class S3SinkPusher implements SinkPusher {

  private static final DateTimeFormatter FILE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").withZone(ZoneId.of("UTC"));

  private final S3Client s3Client;

  public S3SinkPusher(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  @Override
  public Completable pushBatch(
      List<AudienceRecord> records, DataSinkDetails sink, AudienceMeta audience) {
    return Completable.fromAction(
        () -> {
          S3FolderSinkConfig config =
              ConfigurationUtil.parseSinkConfig(sink.getConfig(), S3FolderSinkConfig.class);

          String bucket = config.getBucket();
          String folderPath = normalizeFolderPath(config.getFolderPath());
          String fileFormat = config.getFileFormat() != null ? config.getFileFormat() : "json";
          Long audienceId = audience.getAudienceId();

          // Generate unique filename: audience_{id}_{timestamp}.{format}
          String timestamp = FILE_DATE_FORMAT.format(Instant.now());
          String fileName = String.format("audience_%d_%s.%s", audienceId, timestamp, fileFormat);
          String objectKey = folderPath + fileName;

          log.debug(
              "Uploading {} records to S3 bucket '{}' key '{}' for audience {}",
              records.size(),
              bucket,
              objectKey,
              audienceId);

          // Prepare content based on format
          String content = formatContent(records, fileFormat);
          byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

          // Upload to S3
          PutObjectRequest putRequest =
              PutObjectRequest.builder()
                  .bucket(bucket)
                  .key(objectKey)
                  .contentType(getContentType(fileFormat))
                  .build();

          s3Client.putObject(putRequest, RequestBody.fromBytes(contentBytes));

          log.info(
              "Successfully uploaded {} records to s3://{}/{} for audience {}",
              records.size(),
              bucket,
              objectKey,
              audienceId);
        });
  }

  private String normalizeFolderPath(String folderPath) {
    if (folderPath == null || folderPath.isBlank()) {
      return "";
    }
    // Ensure path ends with /
    return folderPath.endsWith("/") ? folderPath : folderPath + "/";
  }

  private String formatContent(List<AudienceRecord> records, String format) {
    return switch (format.toLowerCase()) {
      case "json" -> formatAsJson(records);
      case "csv" -> formatAsCsv(records);
      default -> formatAsJsonLines(records); // jsonl/ndjson is default
    };
  }

  private String formatAsCsv(List<AudienceRecord> records) {
    // CSV with header
    StringBuilder sb = new StringBuilder();
    sb.append("audienceName,userId,expireDate,action\n");
    for (AudienceRecord record : records) {
      String expireDate = record.getExpireDate() != null ? record.getExpireDate().toString() : "";
      sb.append(
          String.format(
              "%s,%s,%s,%s\n",
              escapeCsv(record.getAudienceName()),
              escapeCsv(record.getUserId()),
              expireDate,
              escapeCsv(record.getAction())));
    }
    return sb.toString();
  }

  private String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private String formatAsJson(List<AudienceRecord> records) {
    // Format as JSON array
    JsonArray recordsArray = new JsonArray();
    for (AudienceRecord record : records) {
      recordsArray.add(record.toJson());
    }
    return new JsonObject()
        .put("recordCount", records.size())
        .put("timestamp", System.currentTimeMillis())
        .put("records", recordsArray)
        .encodePrettily();
  }

  private String formatAsJsonLines(List<AudienceRecord> records) {
    // Format as newline-delimited JSON (one JSON object per line)
    return records.stream()
        .map(record -> record.toJson().encode())
        .collect(Collectors.joining("\n"));
  }

  private String getContentType(String format) {
    return switch (format.toLowerCase()) {
      case "json" -> "application/json";
      case "jsonl", "ndjson" -> "application/x-ndjson";
      default -> "text/csv";
    };
  }

  @Override
  public void close() {
    try {
      log.info("Closing S3 client");
      s3Client.close();
    } catch (Exception e) {
      log.error("Error closing S3 client", e);
    }
  }
}
