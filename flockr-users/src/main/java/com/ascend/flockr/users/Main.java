package com.ascend.flockr.users;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
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
