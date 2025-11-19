package com.ascend.flockr.validation;

import com.ascend.flockr.domain.rule.*;
import com.ascend.flockr.io.request.CreateRulesRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link ValidRuleTypeConfiguration} annotation.
 *
 * <p>Validates that the ruleType field matches the configuration type in the Rule object.
 *
 * @author Flockr Team
 * @since 1.0
 */
public class ValidRuleTypeConfigurationValidator
    implements ConstraintValidator<ValidRuleTypeConfiguration, CreateRulesRequest.Rule> {
  @Override
  public boolean isValid(CreateRulesRequest.Rule rule, ConstraintValidatorContext context) {
    if (rule == null) {
      return true; // Null validation should be handled by @NotNull
    }

    if (rule.getRuleType() == null || rule.getConfiguration() == null) {
      return true; // Null validation should be handled by @NotNull annotations
    }

    RuleType ruleType = rule.getRuleType();
    RuleConfiguration<SourceInfo> configuration = rule.getConfiguration();

    // Check if ruleType matches configuration type
    boolean isValid = false;
    if (ruleType == RuleType.STREAM && configuration instanceof StreamConfiguration) {
      isValid = true;
    } else if (ruleType == RuleType.BATCH && configuration instanceof BatchConfiguration) {
      isValid = true;
    }

    if (!isValid) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              String.format(
                  "Rule type '%s' does not match configuration type '%s'",
                  ruleType, configuration.getClass().getSimpleName().replace("Configuration", "")))
          .addConstraintViolation();
    }

    return isValid;
  }
}
