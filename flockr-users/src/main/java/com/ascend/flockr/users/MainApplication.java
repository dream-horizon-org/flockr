package com.ascend.flockr.users;

import com.ascend.flockr.common.AbstractMainApplication;
import com.ascend.flockr.users.module.AerospikeModule;
import com.ascend.flockr.users.module.DefaultModule;
import com.google.inject.Module;
import io.vertx.core.Vertx;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Main entry point for the flockr-users application.
 *
 * <p>This class serves as the application launcher. In a production environment, this would
 * typically initialize the Vert.x application and start the HTTP server.
 *
 * @since 1.0
 */
@Slf4j
public class MainApplication extends AbstractMainApplication {
  /**
   * Main method to start the application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    log.info("Starting application flockr-users ....");
    MainApplication app = new MainApplication();
    app.dispatch(args);
  }

  @Override
  protected List<Module> getGuiceModules(Vertx vertx) {
    return List.of(new DefaultModule(vertx), new AerospikeModule(vertx));
  }
}
