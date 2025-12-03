package io.ascend.flockr.users.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.List;
import java.util.Objects;
import lombok.Synchronized;

/**
 * Singleton application context providing access to Guice dependency injection container.
 *
 * <p>This class manages a single Guice {@link Injector} instance for the entire application
 * lifecycle. It provides thread-safe access to dependency-injected instances.
 *
 * <p><strong>Initialization:</strong>
 *
 * <p>The context must be initialized once during application startup by calling {@link
 * #initializeContext(List)}. This is typically done in {@link
 * io.ascend.flockr.users.AbstractMainApplication#initializeGuiceInjector(io.vertx.core.Vertx)}.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * // Initialize once at startup
 * AppContext.initializeContext(List.of(new MyServiceModule()));
 *
 * // Get instances anywhere in the application
 * MyService service = AppContext.getInstance(MyService.class);
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong>
 *
 * <p>All methods are thread-safe. Initialization is synchronized to prevent multiple
 * initializations.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
public final class AppContext {

  private static AppContext instance = null;

  private final Injector injector;

  /**
   * Private constructor to create the application context with the provided Guice modules.
   *
   * @param modules the list of Guice modules to configure the injector
   */
  private AppContext(List<com.google.inject.Module> modules) {
    this.injector = Guice.createInjector(modules);
  }

  /**
   * Initializes the application context with the provided Guice modules.
   *
   * <p>This method must be called exactly once during application startup. Subsequent calls will
   * throw an {@link IllegalStateException}.
   *
   * <p><strong>Thread Safety:</strong> This method is synchronized to prevent concurrent
   * initialization.
   *
   * @param modules the list of Guice modules to configure the dependency injection container
   * @throws IllegalStateException if the context has already been initialized
   */
  @Synchronized
  public static void initializeContext(List<Module> modules) {
    if (Objects.nonNull(instance)) {
      throw new IllegalStateException("AppContext is already initialized");
    } else {
      instance = new AppContext(modules);
    }
  }

  /**
   * Returns the singleton AppContext instance.
   *
   * @return the AppContext instance
   * @throws NullPointerException if the context has not been initialized
   */
  private static AppContext instance() {
    Objects.requireNonNull(instance);
    return instance;
  }

  /**
   * Retrieves an instance of the specified class from the Guice injector.
   *
   * <p>This method delegates to the underlying Guice injector to resolve dependencies and return
   * the appropriate instance. The instance is created according to the bindings configured in the
   * modules provided during initialization.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * MyService service = AppContext.getInstance(MyService.class);
   * }</pre>
   *
   * @param <T> the type of instance to retrieve
   * @param clazz the class of the instance to retrieve
   * @return an instance of the specified class, configured according to Guice bindings
   * @throws NullPointerException if the context has not been initialized
   * @throws com.google.inject.ConfigurationException if the requested type is not bound in any
   *     module
   */
  public static <T> T getInstance(Class<T> clazz) {
    return instance().injector.getInstance(clazz);
  }
}
