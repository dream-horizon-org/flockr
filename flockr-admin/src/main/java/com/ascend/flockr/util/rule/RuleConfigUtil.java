package com.ascend.flockr.util.rule;

import com.ascend.flockr.config.*;

public class RuleConfigUtil {

  static RuleConfig ruleConfig;
  static CleverTapConfig cleverTapConfig;
  static UserCohortConfig userCohortConfig;
  static ComaServiceConfig comaServiceConfig;

  static {
    AppConfig appConfig = AppConfigProvider.getAppConfig();
    ruleConfig = appConfig.getRule();
    cleverTapConfig = appConfig.getClevertap();
    userCohortConfig = appConfig.getUserCohort();
    comaServiceConfig = appConfig.getComaService();
  }

  public static Long defaultWatermarkDelay() {
    return ruleConfig.getRealtime().getWatermark();
  }

  public static String defaultRedshiftEngineName() {
    return ruleConfig.getHistoric().getRedshiftEngineName();
  }

  public static RuleConfig defaultRuleConfig() {
    return ruleConfig;
  }

  public static boolean isIcebergStagingPath(String path) {
    return path != null
        && path.startsWith(ruleConfig.getHistoric().getIceberg().getHistoricConfig().getS3path());
  }
}
