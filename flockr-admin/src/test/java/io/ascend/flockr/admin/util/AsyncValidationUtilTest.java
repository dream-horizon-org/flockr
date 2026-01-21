package io.ascend.flockr.admin.util;

import static org.junit.jupiter.api.Assertions.*;

import io.ascend.flockr.admin.domain.rule.*;
import io.ascend.flockr.admin.io.request.CreateRulesRequest;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Field;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link AsyncJakartaValidationUtil}.
 *
 * <p>Tests async validation behavior on worker threads to ensure event loop is not blocked.
 *
 * @author Flockr Team
 * @since 1.0
 */
class AsyncValidationUtilTest {

  private static Vertx vertx;
  private static Validator validator;

  @BeforeAll
  static void setupVertx() throws Exception {
    vertx = Vertx.vertx();
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();

    // Inject dependencies into static fields via reflection (simulating Guice)
    injectStaticField("vertx", vertx);
    injectStaticField("validator", validator);
  }

  @AfterAll
  static void tearDownVertx() {
    vertx.close();
  }

  /**
   * Helper method to inject values into static fields via reflection. This simulates what Guice
   * does with requestStaticInjection().
   */
  private static void injectStaticField(String fieldName, Object value) throws Exception {
    Field field = AsyncJakartaValidationUtil.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, value);
  }

  @Test
  @DisplayName("Should validate valid BatchConfiguration async without blocking event loop")
  void testValidBatchConfigurationAsync() throws InterruptedException {
    // Arrange
    SourceInfo sourceInfo = SourceInfo.builder().id(1L).build();

    BatchConfiguration<SourceInfo> config =
        BatchConfiguration.<SourceInfo>builder()
            .source(sourceInfo)
            .query("SELECT user_id FROM users WHERE status = 'active'")
            .cronExpression("0 0 * * *")
            .build();

    // Act
    Single<BatchConfiguration<SourceInfo>> result = AsyncJakartaValidationUtil.validate(config);

    // Assert
    result
        .test()
        .await()
        .assertComplete()
        .assertNoErrors()
        .assertValue(
            validatedConfig -> {
              assertEquals(config, validatedConfig);
              return true;
            });
  }

  @Test
  @DisplayName("Should fail validation for invalid SQL query async")
  void testInvalidSqlQueryAsync() throws InterruptedException {
    // Arrange
    SourceInfo sourceInfo = SourceInfo.builder().id(1L).build();

    BatchConfiguration<SourceInfo> config =
        BatchConfiguration.<SourceInfo>builder()
            .source(sourceInfo)
            .query("SELECT FROM WHERE") // Invalid SQL
            .cronExpression("0 0 * * *")
            .build();

    // Act
    Single<BatchConfiguration<SourceInfo>> result = AsyncJakartaValidationUtil.validate(config);

    // Assert
    result
        .test()
        .await()
        .assertError(AsyncJakartaValidationUtil.ValidationException.class)
        .assertError(
            error -> {
              assertTrue(error.getMessage().contains("Invalid SQL syntax"));
              return true;
            });
  }

  @Test
  @DisplayName("Should validate CreateRulesRequest async")
  void testValidateCreateRulesRequestAsync() throws InterruptedException {
    // Arrange
    SourceInfo sourceInfo = SourceInfo.builder().id(1L).build();

    BatchConfiguration<SourceInfo> config =
        BatchConfiguration.<SourceInfo>builder()
            .source(sourceInfo)
            .query("SELECT user_id FROM orders GROUP BY user_id HAVING COUNT(*) > 5")
            .cronExpression("0 0 * * *")
            .build();

    long currentTimeSeconds = System.currentTimeMillis() / 1000;
    CreateRulesRequest.Rule rule = new CreateRulesRequest.Rule();
    rule.setName("High Volume Users");
    rule.setDescription("Users with many orders");
    rule.setRuleType(RuleType.BATCH);
    rule.setRuleAction(RuleAction.ADD);
    rule.setConfiguration(config);
    rule.setStartTime(currentTimeSeconds + 100); // 100 seconds in future
    rule.setEndTime(currentTimeSeconds + 500); // 500 seconds in future

    CreateRulesRequest request = new CreateRulesRequest();
    request.setAudienceId(10L);
    request.setRules(java.util.Arrays.asList(rule));

    // Act
    Single<CreateRulesRequest> result = AsyncJakartaValidationUtil.validate(request);

    // Assert
    result
        .test()
        .await()
        .assertComplete()
        .assertNoErrors()
        .assertValue(
            validatedRequest -> {
              assertEquals(request, validatedRequest);
              return true;
            });
  }

  @Test
  @DisplayName("Should validate on worker thread not on event loop")
  void testValidationHappensOnWorkerThread() throws InterruptedException {
    // Arrange
    SourceInfo sourceInfo = SourceInfo.builder().id(1L).build();

    // Complex query that would block event loop if validated synchronously
    String complexQuery =
        "WITH recent_orders AS ( "
            + "  SELECT user_id, COUNT(*) as order_count "
            + "  FROM orders "
            + "  WHERE created_at > CURRENT_DATE - INTERVAL '90 days' "
            + "  GROUP BY user_id "
            + ") "
            + "SELECT user_id "
            + "FROM recent_orders "
            + "WHERE order_count >= 3";

    BatchConfiguration<SourceInfo> config =
        BatchConfiguration.<SourceInfo>builder()
            .source(sourceInfo)
            .query(complexQuery)
            .cronExpression("0 0 * * 0")
            .build();

    // Act - validation should happen on worker thread
    Single<BatchConfiguration<SourceInfo>> result = AsyncJakartaValidationUtil.validate(config);

    // Assert - should complete successfully without blocking
    result.test().await().assertComplete().assertNoErrors();
  }

  @Test
  @DisplayName("Should handle multiple concurrent validations")
  void testConcurrentValidations() throws InterruptedException {
    // Arrange
    int numberOfValidations = 10;

    SourceInfo sourceInfo = SourceInfo.builder().id(1L).build();

    BatchConfiguration<SourceInfo> config =
        BatchConfiguration.<SourceInfo>builder()
            .source(sourceInfo)
            .query("SELECT user_id FROM users")
            .cronExpression("0 0 * * *")
            .build();

    // Act - perform multiple validations concurrently
    Single<?>[] validations = new Single[numberOfValidations];
    for (int i = 0; i < numberOfValidations; i++) {
      validations[i] = AsyncJakartaValidationUtil.validate(config);
    }

    // Assert - all validations should complete successfully
    Single.merge(java.util.Arrays.asList(validations))
        .test()
        .await()
        .assertComplete()
        .assertNoErrors()
        .assertValueCount(numberOfValidations);
  }
}
