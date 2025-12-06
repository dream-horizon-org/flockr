package io.ascend.flockr.admin.service;

import io.ascend.flockr.admin.io.request.CsvImportForm;
import io.ascend.flockr.admin.io.response.AudienceImportResponse;
import io.reactivex.rxjava3.core.Single;

/**
 * Service interface for managing CSV imports for STATIC audiences.
 *
 * <p>This is a fire-and-forget API - no import history is stored. The CSV data is streamed and
 * pushed directly to configured sinks.
 */
public interface AudienceImportService {

  /**
   * Processes a CSV import for a STATIC audience.
   *
   * <p>This method validates that the audience is of type STATIC, streams through the CSV data, and
   * pushes records to the configured sinks in batches.
   *
   * @param xProjectId the encrypted project identifier
   * @param audienceId the audience identifier
   * @param form the multipart form containing the CSV file
   * @param actor the email/username of the user initiating the import
   * @return a Single emitting the import result (record count and status)
   */
  Single<AudienceImportResponse> createImport(
      String xProjectId, Long audienceId, CsvImportForm form, String actor);
}
