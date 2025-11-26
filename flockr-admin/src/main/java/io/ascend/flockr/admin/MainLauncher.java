package io.ascend.flockr.admin;

import com.ascend.flockr.injection.GuiceInjector;
import com.ascend.flockr.injection.module.ServiceModule;
import com.ascend.flockr.util.CommonUtil;
import com.google.inject.Module;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Main launcher class for the Flockr application. Extends Vert.x Launcher to provide custom
 * initialization logic for Vert.x options, Guice dependency injection, and verticle deployment.
 *
 * <p>This class handles:
 *
 * <ul>
 *   <li>Configuring Vert.x event loop pool size based on available CPU cores
 *   <li>Initializing Guice dependency injection after Vert.x starts
 *   <li>Configuring verticle deployment options
 *   <li>Handling deployment failures
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
public class MainLauncher extends Launcher {

  /**
   * Main entry point for the application.
   *
   * @param args command line arguments passed to the application
   */
  public static void main(String[] args) {
    log.info("Starting Application..........");
    new MainLauncher().dispatch(args);
  }

  /**
   * Configures Vert.x options before starting the Vert.x instance.
   *
   * <p>Sets the event loop pool size to match the number of available CPU cores, enables native
   * transport, and configures Dropwizard metrics with JMX enabled.
   *
   * @param vertxOptions the Vert.x options to configure
   */
  @Override
  public void beforeStartingVertx(VertxOptions vertxOptions) {
    vertxOptions
        .setEventLoopPoolSize(CommonUtil.getNumberOfCores())
        .setPreferNativeTransport(true)
        .setMetricsOptions(new DropwizardMetricsOptions().setJmxEnabled(true));
  }

  /**
   * Initializes Guice dependency injection after Vert.x instance has started.
   *
   * @param vertx the Vert.x instance that was started
   */
  @Override
  public void afterStartingVertx(Vertx vertx) {
    log.info("Initializing Guice Modules..........");
    initializeGuiceInjector(vertx);
  }

  /**
   * Configures deployment options before deploying verticles.
   *
   * <p>Sets the number of instances to 1 for each verticle deployment.
   *
   * @param deploymentOptions the deployment options to configure
   */
  @Override
  public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    log.info("Deploying Verticles..........");
    deploymentOptions.setInstances(1);
  }

  /**
   * Handles verticle deployment failures by logging the error and delegating to the parent handler.
   *
   * @param vertx the Vert.x instance
   * @param mainVerticle the name of the verticle that failed to deploy
   * @param deploymentOptions the deployment options that were used
   * @param cause the exception that caused the deployment failure
   */
  @Override
  public void handleDeployFailed(
      Vertx vertx, String mainVerticle, DeploymentOptions deploymentOptions, Throwable cause) {
    log.error(
        "Deployment of {} verticle failed with options {} due to error: ",
        mainVerticle,
        deploymentOptions,
        cause);
    super.handleDeployFailed(vertx, mainVerticle, deploymentOptions, cause);
  }

  /**
   * Initializes the Guice injector with the required modules.
   *
   * @param vertx the Vert.x instance to use for module initialization
   */
  static void initializeGuiceInjector(Vertx vertx) {
    GuiceInjector.initializeInjector(getGuiceModules(vertx));
  }

  /**
   * Gets the list of Guice modules to be used for dependency injection.
   *
   * @param vertx the Vert.x instance to pass to the modules
   * @return a list of Guice modules
   */
  private static List<Module> getGuiceModules(Vertx vertx) {
    return List.of(new ServiceModule(io.vertx.rxjava3.core.Vertx.newInstance(vertx)));
  }
}
