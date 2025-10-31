package com.ascend.flockr.common;

import com.ascend.flockr.common.guice.AppContext;
import com.ascend.flockr.common.guice.GuiceInjector;
import com.google.inject.Module;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMainApplication extends Launcher {
  public static void main(String[] args) {
    throw new UnsupportedOperationException("main() method is supported only through inheritors");
  }

  @Override
  public void beforeStartingVertx(VertxOptions vertxOptions) {
    vertxOptions
        .setEventLoopPoolSize(CpuCoreSensor.availableProcessors())
        .setPreferNativeTransport(true)
        .setMetricsOptions(new DropwizardMetricsOptions().setJmxEnabled(true));
  }

  @Override
  public void afterStartingVertx(Vertx vertx) {
    log.info("Initializing Guice Modules..........");
    this.initializeGuiceInjector(vertx);
  }

  @Override
  public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    log.info("Deploying Verticles..........");
    deploymentOptions.setInstances(1);
  }

  @Override
  public void handleDeployFailed(
      Vertx vertx, String mainVerticle, DeploymentOptions deploymentOptions, Throwable cause) {
    log.error(
        "Deployment of main-verticle {} with options {} failed due to error: ",
        mainVerticle,
        deploymentOptions,
        cause);
    super.handleDeployFailed(vertx, mainVerticle, deploymentOptions, cause);
  }

  protected void initializeGuiceInjector(Vertx vertx) {
    AppContext.initializeContext(getGuiceModules(vertx));
    VertxContextUtils.setInstanceInSharedData(vertx, new GuiceInjector());
  }

  protected abstract List<Module> getGuiceModules(Vertx vertx);
}
