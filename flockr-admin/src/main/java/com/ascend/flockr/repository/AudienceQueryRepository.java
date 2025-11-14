package com.ascend.flockr.repository;

import com.ascend.flockr.io.response.AudienceDetailsResponse;
import io.reactivex.rxjava3.core.Single;

/**
 * Repository for querying audience details with all related data including sinks, rules, and
 * sources. This repository is optimized for UI/query operations.
 */
public interface AudienceQueryRepository {
  /**
   * Get complete audience details including: - Audience metadata - Associated data sinks with their
   * configuration - All rules with their configuration - For each rule, extract and fetch source
   * metadata based on rule type
   *
   * @param audienceId the ID of the audience
   * @return Complete audience details with all nested data
   */
  Single<AudienceDetailsResponse> getAudienceDetails(Long audienceId);
}
