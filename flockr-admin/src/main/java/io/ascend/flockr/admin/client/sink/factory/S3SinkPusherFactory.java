package io.ascend.flockr.admin.client.sink.factory;

import io.ascend.flockr.admin.client.sink.pusher.S3SinkPusher;
import io.ascend.flockr.admin.client.sink.pusher.SinkPusher;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.config.S3FolderSinkConfig;
import io.ascend.flockr.admin.util.ConfigParser;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
public class S3SinkPusherFactory implements SinkPusherFactory {
  private static final String SINK_TYPE = "S3_FOLDER";

  private final Map<String, S3SinkPusher> pusherCache = new ConcurrentHashMap<>();

  @Override
  public String getSinkType() {
    return SINK_TYPE;
  }

  @Override
  public SinkPusher create(DataSinkDetails sink) {
    S3FolderSinkConfig config =
        ConfigParser.parseSinkConfig(sink.getConfig(), S3FolderSinkConfig.class);
    return getOrCreatePusher(config);
  }

  private S3SinkPusher getOrCreatePusher(S3FolderSinkConfig config) {
    String cacheKey = buildCacheKey(config);
    return pusherCache.computeIfAbsent(
        cacheKey,
        key -> {
          S3Client client = createS3Client(config);
          log.info("Creating new S3 pusher for cache key: {}", key);
          return new S3SinkPusher(client);
        });
  }

  private S3Client createS3Client(S3FolderSinkConfig config) {
    var builder = S3Client.builder();

    // Set region if provided
    if (config.getRegion() != null && !config.getRegion().isBlank()) {
      builder.region(Region.of(config.getRegion()));
    }

    // Set credentials if provided, otherwise use default credential chain
    if (config.getAccessKey() != null
        && !config.getAccessKey().isBlank()
        && config.getSecretKey() != null
        && !config.getSecretKey().isBlank()) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())));
      log.info("Creating S3 client with explicit credentials for region: {}", config.getRegion());
    } else {
      builder.credentialsProvider(DefaultCredentialsProvider.create());
      log.info("Creating S3 client with default credentials for region: {}", config.getRegion());
    }

    return builder.build();
  }

  private String buildCacheKey(S3FolderSinkConfig config) {
    // Cache key based on region and whether explicit credentials are used
    String region = config.getRegion() != null ? config.getRegion() : "default";
    String credType =
        (config.getAccessKey() != null && !config.getAccessKey().isBlank())
            ? "explicit"
            : "default";
    return region + ":" + credType;
  }

  /** Closes all cached S3 pushers. Call this on application shutdown. */
  public void close() {
    pusherCache.forEach(
        (key, pusher) -> {
          log.info("Closing S3 pusher for: {}", key);
          pusher.close();
        });
    pusherCache.clear();
  }
}
