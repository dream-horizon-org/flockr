package com.ascend.flockr.common.guice;

import com.dream11.rest.ClassInjector;

public class GuiceInjector implements ClassInjector {
  @Override
  public <T> T getInstance(Class<T> clazz) {
    return AppContext.getInstance(clazz);
  }
}
