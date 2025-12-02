package io.ascend.flockr.admin.io.request;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import lombok.Data;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

/**
 * Multipart form data class for CSV file uploads.
 *
 * <p>This class is used to receive CSV files via multipart/form-data requests. The uploaded file is
 * available as an InputStream for processing.
 *
 * @since 1.0
 */
@Data
public class CsvImportForm {

  /** The CSV file content as an input stream. */
  @FormParam("file")
  @PartType(MediaType.APPLICATION_OCTET_STREAM)
  private InputStream file;

  /** The original file name of the uploaded CSV. */
  @FormParam("fileName")
  @PartType(MediaType.TEXT_PLAIN)
  private String fileName;
}
