package com.ascend.flockr.users;

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
public class Main {
  /**
   * Main method to start the application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    log.info("Starting application flockr-users ....");
    //    new MainApplication().dispatchargs();

    for (int i = 1; i <= 5; i++) {
      // TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon
      // src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
      // for you, but you can always add more by pressing <shortcut
      // actionId="ToggleLineBreakpoint"/>.
      System.out.println("i = " + i);
    }
  }
}
