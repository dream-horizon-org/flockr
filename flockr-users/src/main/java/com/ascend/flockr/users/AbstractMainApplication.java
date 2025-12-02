package com.ascend.flockr.users;

import com.ascend.flockr.users.guice.AppContext;
import com.google.inject.Module;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for main application launchers in the Flockr platform.
 *
 * <p>This class extends Vert.x {@link Launcher} and provides a framework for initializing Vert.x
 * applications with Guice dependency injection. It handles common setup tasks such as:
 *
 * <ul>
 *   <li>Configuring Vert.x options (event loop pool size, native transport, metrics)
 *   <li>Initializing Guice modules after Vert.x starts
 *   <li>Configuring verticle deployment options
 *   <li>Handling deployment failures with proper error logging
 * </ul>
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * public class MyApplication extends AbstractMainApplication {
 *   public static void main(String[] args) {
 *     new MyApplication().dispatch(args);
 *   }
 *
 *   {@literal @}Override
 *   protected List<Module> getGuiceModules(Vertx vertx) {
 *     return List.of(new MyServiceModule(vertx));
 *   }
 * }
 * }</pre>
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
public abstract class AbstractMainApplication extends Launcher {
  /**
   * Main entry point that should not be called directly.
   *
   * <p>This method throws an exception to prevent direct instantiation. Subclasses should override
   * this method and delegate to {@link #dispatch(String[])} instead.
   *
   * @param args command line arguments
   * @throws UnsupportedOperationException always thrown to prevent direct usage
   */
  public static void main(String[] args) {
    throw new UnsupportedOperationException("main() method is supported only through inheritors");
  }

  /**
   * Configures Vert.x options before the Vert.x instance is created.
   *
   * <p>This method sets:
   *
   * <ul>
   *   <li>Event loop pool size to match available CPU cores
   *   <li>Native transport preference for better performance
   *   <li>Dropwizard metrics with JMX enabled for monitoring
   * </ul>
   *
   * @param vertxOptions the Vert.x options to configure
   */
  @Override
  public void beforeStartingVertx(VertxOptions vertxOptions) {
    vertxOptions
        .setEventLoopPoolSize(CpuCoreSensor.availableProcessors())
        .setPreferNativeTransport(true)
        .setMetricsOptions(new DropwizardMetricsOptions().setJmxEnabled(true));
  }

  /**
   * Initializes Guice dependency injection after Vert.x starts.
   *
   * <p>This method is called after the Vert.x instance is created but before verticles are
   * deployed. It initializes the Guice injector with modules provided by {@link
   * #getGuiceModules(Vertx)}.
   *
   * @param vertx the Vert.x instance that was just created
   */
  @Override
  public void afterStartingVertx(Vertx vertx) {
    log.info("Initializing Guice Modules..........");
    this.initializeGuiceInjector(vertx);
  }

  /**
   * Configures deployment options before deploying verticles.
   *
   * <p>Sets the number of verticle instances to 1 by default. Subclasses can override this method
   * to customize deployment options.
   *
   * @param deploymentOptions the deployment options to configure
   */
  @Override
  public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    log.info("Deploying Verticles..........");
    deploymentOptions.setInstances(1);
  }

  /**
   * Handles verticle deployment failures with error logging.
   *
   * <p>This method logs detailed error information about failed deployments and delegates to the
   * parent class for default error handling.
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
        "Deployment of main-verticle {} with options {} failed due to error: ",
        mainVerticle,
        deploymentOptions,
        cause);
    super.handleDeployFailed(vertx, mainVerticle, deploymentOptions, cause);
  }

  /**
   * Initializes the Guice injector with the provided modules.
   *
   * <p>This method creates the application context using the Guice modules returned by {@link
   * #getGuiceModules(Vertx)}. The context is initialized as a singleton and can be accessed via
   * {@link AppContext#getInstance(Class)}.
   *
   * @param vertx the Vert.x instance, passed to module constructors if needed
   */
  protected void initializeGuiceInjector(Vertx vertx) {
    AppContext.initializeContext(getGuiceModules(vertx));
    //        VertxContextUtils.setInstanceInSharedData(vertx, new GuiceInjector());
  }

  /**
   * Returns the list of Guice modules to use for dependency injection.
   *
   * <p>Subclasses must implement this method to provide their application-specific Guice modules.
   * These modules will be used to configure the dependency injection container.
   *
   * @param vertx the Vert.x instance, can be used to create Vert.x-specific modules
   * @return a list of Guice modules to initialize
   */
  protected abstract List<Module> getGuiceModules(Vertx vertx);
}

