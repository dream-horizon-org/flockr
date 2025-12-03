package com.ascend.flockr.users.guice;

import com.dream11.rest.ClassInjector;

/**
 * Adapter class that implements {@link ClassInjector} using the Guice-based {@link AppContext}.
 *
 * <p>This class provides a bridge between the Dream11 REST framework's {@link ClassInjector}
 * interface and the Flockr platform's Guice dependency injection system. It delegates all instance
 * retrieval to {@link AppContext#getInstance(Class)}.
 *
 * <p><strong>Usage:</strong>
 *
 * <p>This class is typically used by the REST framework to resolve dependencies. It should be
 * configured in the application's Guice modules.
 *
 * @author Flockr Team
 * @since 1.0
 */
public class GuiceInjector implements ClassInjector {
  /**
   * Retrieves an instance of the specified class from the Guice application context.
   *
   * <p>This method delegates to {@link AppContext#getInstance(Class)} to resolve the instance using
   * Guice dependency injection.
   *
   * @param <T> the type of instance to retrieve
   * @param clazz the class of the instance to retrieve
   * @return an instance of the specified class, configured according to Guice bindings
   */
  @Override
  public <T> T getInstance(Class<T> clazz) {
    return AppContext.getInstance(clazz);
  }
}
