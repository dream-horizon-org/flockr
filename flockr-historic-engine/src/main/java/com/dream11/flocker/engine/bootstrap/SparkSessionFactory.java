package com.dream11.flocker.engine.bootstrap;

import com.dream11.flocker.engine.config.SparkConfig;
import com.dream11.flocker.engine.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

/**
 * Factory for creating and configuring Spark sessions.
 *
 * <p>This class is responsible for:
 *
 * <ul>
 *   <li>Determining the Spark master URL (from system property or default)
 *   <li>Loading Spark configuration from config files
 *   <li>Creating and configuring SparkSession instances
 * </ul>
 *
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class SparkSessionFactory {

  /**
   * Creates a new SparkSession with the configured settings.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Determines Spark master URL (from system property or default)
   *   <li>Loads Spark configuration from config files
   *   <li>Applies configuration to SparkSession builder
   *   <li>Creates and returns the SparkSession
   * </ol>
   *
   * @return A configured SparkSession instance.
   */
  public SparkSession createSparkSession() {
    String sparkMaster = getSparkMaster();
    log.info("Spark Master: {}", sparkMaster);

    SparkConfig sparkConfig = SparkConfig.provider().get();
    log.debug("Loaded Spark configuration from config file");

    SparkSession.Builder sessionBuilder = SparkSession.builder();
    sessionBuilder.master(sparkMaster);
    sparkConfig.applyToSessionBuilder(sessionBuilder);

    SparkSession sparkSession = sessionBuilder.getOrCreate();
    log.debug("SparkSession created successfully with master: {}", sparkMaster);

    return sparkSession;
  }

  /**
   * Gets the Spark master URL from system property or returns the default.
   *
   * @return The Spark master URL.
   */
  private String getSparkMaster() {
    return System.getProperty(
        Constants.KEY_SPARK_MASTER,
        "spark://" + Constants.FLOCKR_SPARK_MASTER + ":" + Constants.FLOCKR_SPARK_MASTER_PORT);
  }
}
