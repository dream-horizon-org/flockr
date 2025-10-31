package com.ascend.flockr.common.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.List;
import java.util.Objects;
import lombok.Synchronized;

public final class AppContext {

  private static AppContext instance = null;

  private final Injector injector;

  private AppContext(List<com.google.inject.Module> modules) {
    this.injector = Guice.createInjector(modules);
  }

  @Synchronized
  public static void initializeContext(List<Module> modules) {
    if (Objects.nonNull(instance)) {
      throw new IllegalStateException("AppContext is already initialized");
    } else {
      instance = new AppContext(modules);
    }
  }

  private static AppContext instance() {
    Objects.requireNonNull(instance);
    return instance;
  }

  public static <T> T getInstance(Class<T> clazz) {
    return instance().injector.getInstance(clazz);
  }
}
