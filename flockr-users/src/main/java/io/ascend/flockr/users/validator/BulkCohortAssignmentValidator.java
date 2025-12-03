package io.ascend.flockr.users.validator;

import com.dream11.rest.util.ExceptionUtil;
import io.ascend.flockr.users.exception.errors.DefinedErrors;
import java.io.IOException;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

/**
 * Validator for bulk cohort assignment multipart form data.
 *
 * <p>Validates that required form fields (cohort_name and csv_file) are present in the multipart
 * form data.
 *
 * @author Sudhanshu Rai
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class BulkCohortAssignmentValidator {

  private static final String CSV_FILE = "csv_file";
  private static final String COHORT_NAME = "cohort_name";

  /**
   * Validates and extracts the cohort_name from multipart form data.
   *
   * @param input the multipart form data input
   * @return the cohort name as a trimmed string
   * @throws RuntimeException if cohort_name is missing or empty
   * @throws RuntimeException if IOException occurs (wrapped)
   */
  public static String validateAndExtractCohortName(MultipartFormDataInput input) {
    InputPart part = getPart(input, COHORT_NAME);
    String cohortName;
    try {
      cohortName = part.getBodyAsString();
    } catch (IOException e) {
      log.error("Error reading cohort_name from form data", e);
      throw new RuntimeException(e);
    }
    if (cohortName == null) {
      log.error("Missing cohort_name in form data");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_COHORT_NAME);
    }
    String trimmed = cohortName.trim();
    if (trimmed.isEmpty()) {
      log.error("Empty cohort_name in form data");
      throw ExceptionUtil.getException(DefinedErrors.MISSING_COHORT_NAME);
    }
    return trimmed;
  }

  /**
   * Validates and extracts the csv_file from multipart form data.
   *
   * @param input the multipart form data input
   * @return the csv_file InputPart
   * @throws RuntimeException if csv_file is missing
   */
  public static InputPart validateAndExtractCsvFile(MultipartFormDataInput input) {
    return getPart(input, CSV_FILE);
  }

  /**
   * Gets the first input part with the specified name from multipart form data.
   *
   * @param input the multipart form data input
   * @param name the name of the form field
   * @return the first InputPart with the given name
   * @throws RuntimeException if the part is missing
   */
  private static InputPart getPart(MultipartFormDataInput input, String name) {
    List<InputPart> parts = input.getFormDataMap().get(name);
    if (parts == null || parts.isEmpty()) {
      if (CSV_FILE.equals(name)) {
        throw ExceptionUtil.getException(DefinedErrors.MISSING_CSV_FILE);
      } else if (COHORT_NAME.equals(name)) {
        throw ExceptionUtil.getException(DefinedErrors.MISSING_COHORT_NAME);
      } else {
        // Fallback for unknown form fields - should not happen in normal operation
        throw ExceptionUtil.getException(
            DefinedErrors.INVALID_REQUEST, "Missing required form field: " + name);
      }
    }
    return parts.get(0);
  }
}
