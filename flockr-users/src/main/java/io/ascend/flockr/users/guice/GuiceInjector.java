package io.ascend.flockr.users.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.List;
import java.util.Objects;
import lombok.Synchronized;

/**
 * @author Sudhanshu Rai
 * @since 1.0
 */
public class GuiceInjector {

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
   * <p>This method can only be called once. Subsequent calls will throw an IllegalStateException.
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
