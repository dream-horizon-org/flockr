package com.ascend.flockr.injection;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.List;
import java.util.Objects;
import lombok.Synchronized;

/**
 * Singleton wrapper for Guice dependency injection container.
 *
 * <p>This class provides a thread-safe way to initialize and access the Guice injector. The injector
 * must be initialized once before use, typically during application startup.
 *
 * <p><strong>Thread Safety:</strong> All methods are synchronized to ensure thread-safe access to
 * the singleton instance.
 *
 * @author Flockr Team
 * @since 1.0
 */
public final class GuiceInjector {

  private static GuiceInjector instance = null;
  private final Injector injector;

  /**
   * Constructs a new GuiceInjector with the provided modules.
   *
   * @param modules the list of Guice modules to configure the injector
   */
  private GuiceInjector(List<Module> modules) {
    this.injector = Guice.createInjector(modules);
  }

  /**
   * Initializes the Guice injector with the provided modules.
   *
   * <p>This method can only be called once. Subsequent calls will throw an
   * IllegalStateException.
   *
   * @param modules the list of Guice modules to configure the injector
   * @throws IllegalStateException if the injector has already been initialized
   */
  @Synchronized
  public static void initializeInjector(List<Module> modules) {
    if (Objects.nonNull(instance)) {
      throw new IllegalStateException("GuiceInjector is already initialized");
    } else {
      instance = new GuiceInjector(modules);
    }
  }

  /**
   * Gets the singleton instance of GuiceInjector.
   *
   * @return the GuiceInjector instance
   * @throws NullPointerException if the injector has not been initialized
   */
  private static GuiceInjector instance() {
    return Objects.requireNonNull(instance);
  }

  /**
   * Gets an instance of the specified type from the Guice injector.
   *
   * @param clazz the class of the instance to retrieve
   * @param <T> the type of instance to retrieve
   * @return an instance of the specified type
   * @throws NullPointerException if the injector has not been initialized
   * @throws com.google.inject.ConfigurationException if the type cannot be provided by the injector
   */
  public static <T> T getInstance(Class<T> clazz) {
    return instance().injector.getInstance(clazz);
  }
}
