package com.ascend.flockr.verticle;

import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.client.postgres.PostgresWriterClient;
import com.ascend.flockr.client.webclient.WebClient;
import com.ascend.flockr.constants.Constants;
import com.ascend.flockr.injection.GuiceInjector;
import com.ascend.flockr.util.CommonUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.rxjava3.core.AbstractVerticle;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Main verticle that orchestrates the deployment of all other verticles in the application.
 *
 * <p>This verticle is responsible for:
 *
 * <ul>
 *   <li>Deploying REST API verticles with appropriate instance counts and worker pool sizes
 *   <li>Gracefully shutting down all clients (PostgreSQL readers/writers, WebClient) when stopped
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
public class MainVerticle extends AbstractVerticle {

  /**
   * Starts the main verticle by deploying all configured verticles.
   *
   * <p>Deploys verticles sequentially and logs any deployment errors. The deployment completes when
   * all verticles are successfully deployed.
   *
   * @return a Completable that completes when all verticles are deployed, or errors if deployment
   *     fails
   */
  @Override
  public Completable rxStart() {
    return Observable.fromIterable(this.getVerticleDeployments())
        .flatMapSingle(
            verticleDeployment ->
                vertx.rxDeployVerticle(
                    verticleDeployment.verticleSupplier(), verticleDeployment.deploymentOptions()))
        .ignoreElements()
        .doOnError(err -> log.error("Failed to deploy verticles due to error: ", err))
        .doOnComplete(() -> log.info("Deployed all verticles. Started Application.........."));
  }

  /**
   * Stops the main verticle by closing all clients gracefully.
   *
   * @return a Completable that completes when all clients are closed
   */
  @Override
  public Completable rxStop() {
    return stopClients();
  }

  /**
   * Gets the list of verticles to be deployed along with their deployment options.
   *
   * @return a list of verticle deployments configured for the application
   */
  private List<VerticleDeployment> getVerticleDeployments() {
    return List.of(
        new VerticleDeployment(
            () -> GuiceInjector.getInstance(RestVerticle.class),
            new DeploymentOptions()
                .setInstances(
                    Math.min(CommonUtil.getNumberOfCores(), Constants.MAX_NUM_REST_VERTICLES))
                .setWorkerPoolSize(40)));
  }

  /**
   * Record representing a verticle deployment configuration.
   *
   * @param verticleSupplier supplier that provides the verticle instance to deploy
   * @param deploymentOptions options for deploying the verticle
   */
  record VerticleDeployment(
      Supplier<Verticle> verticleSupplier, DeploymentOptions deploymentOptions) {}

  /**
   * Closes all clients used by the application (PostgreSQL readers/writers, WebClient).
   *
   * @return a Completable that completes when all clients are closed
   */
  private Completable stopClients() {
    PostgresReaderClient mySQLReaderClient = GuiceInjector.getInstance(PostgresReaderClient.class);
    PostgresWriterClient mySQLWriterClient = GuiceInjector.getInstance(PostgresWriterClient.class);
    WebClient webClient = GuiceInjector.getInstance(WebClient.class);

    return Completable.mergeArray(
        mySQLReaderClient.close(), mySQLWriterClient.close(), webClient.close());
  }
}
