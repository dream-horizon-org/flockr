package com.ascend.flockr.users.verticle;

import com.ascend.flockr.users.config.HttpServerConfig;
import com.ascend.flockr.users.constants.Constants;
import com.ascend.flockr.users.guice.AppContext;
import com.ascend.flockr.users.util.CommonUtils;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.rxjava3.core.AbstractVerticle;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainVerticle extends AbstractVerticle {
  private static final String PACKAGE_NAME = "com.ascend.flockr.users";
  private final HttpServerConfig httpServerConfig = AppContext.getInstance(HttpServerConfig.class);

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

  private List<VerticleDeployment> getVerticleDeployments() {
    return List.of(
        new VerticleDeployment(
            () -> AppContext.getInstance(RestVerticle.class),
            new DeploymentOptions()
                .setInstances(
                    Math.min(CommonUtils.getNumOfCores(), Constants.MAX_NUM_REST_VERTICLES))
                .setWorkerPoolSize(40)));
  }

  record VerticleDeployment(
      Supplier<Verticle> verticleSupplier, DeploymentOptions deploymentOptions) {}
}
