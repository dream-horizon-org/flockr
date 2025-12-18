package io.ascend.flockr.admin.config;

import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for encryption keys used in the double-layer encryption system.
 *
 * <p>This configuration manages two encryption keys:
 *
 * <ul>
 *   <li><strong>ENCRYPTION_KEY:</strong> Shared with frontend, used to decrypt frontend-encrypted
 *       values (Layer 1)
 *   <li><strong>BACKEND_STORAGE_ENCRYPTION_KEY:</strong> Backend-only, used to encrypt data at rest
 *       in the database (Layer 2)
 * </ul>
 *
 * <p>Keys are expected to be Base64-encoded 256-bit (32-byte) values stored in environment
 * variables.
 *
 * <p><strong>Security Notes:</strong>
 *
 * <ul>
 *   <li>Keys are loaded once at startup and cached in memory
 *   <li>Keys are never logged or exposed in error messages
 *   <li>Missing keys will cause application startup to fail
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
@Singleton
public class EncryptionConfig {

  /** Environment variable name for frontend-shared encryption key. */
  private static final String ENCRYPTION_KEY_ENV = "ENCRYPTION_KEY";

  /** Environment variable name for backend storage encryption key. */
  private static final String BACKEND_STORAGE_ENCRYPTION_KEY_ENV = "BACKEND_STORAGE_ENCRYPTION_KEY";

  /**
   * Frontend-shared encryption key used to decrypt Layer 1 (frontend-encrypted) values.
   *
   * <p>This key is shared between frontend and backend for decrypting credentials that were
   * encrypted by the frontend before transmission.
   */
  @Getter private final String encryptionKey;

  /**
   * Backend-only encryption key used for Layer 2 (database storage) encryption.
   *
   * <p>This key is only known to the backend and is used to re-encrypt credentials before storing
   * them in the database.
   */
  @Getter private final String backendStorageEncryptionKey;

  /**
   * Constructs an EncryptionConfig by loading keys from environment variables.
   *
   * @throws IllegalStateException if either encryption key is missing or invalid
   */
  public EncryptionConfig() {
    log.info("Initializing encryption configuration...");

    this.encryptionKey = loadKey(ENCRYPTION_KEY_ENV, "frontend-shared");
    this.backendStorageEncryptionKey =
        loadKey(BACKEND_STORAGE_ENCRYPTION_KEY_ENV, "backend storage");

    validateKey(this.encryptionKey, ENCRYPTION_KEY_ENV);
    validateKey(this.backendStorageEncryptionKey, BACKEND_STORAGE_ENCRYPTION_KEY_ENV);

    log.info("Encryption configuration initialized successfully");
  }

  /**
   * Loads an encryption key from an environment variable.
   *
   * @param envVarName the name of the environment variable
   * @param keyDescription a description of the key for logging purposes
   * @return the encryption key value
   * @throws IllegalStateException if the environment variable is not set
   */
  private String loadKey(String envVarName, String keyDescription) {
    String key = System.getenv(envVarName);

    if (key == null || key.trim().isEmpty()) {
      String errorMsg =
          String.format(
              "Encryption key '%s' (%s) is not configured. "
                  + "Please set the %s environment variable with a Base64-encoded 256-bit key.",
              keyDescription, envVarName, envVarName);
      log.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }

    log.debug("Loaded {} encryption key from environment variable {}", keyDescription, envVarName);
    return key.trim();
  }

  /**
   * Validates that an encryption key is properly formatted.
   *
   * <p>Checks that the Base64-decoded key is exactly 32 bytes (256 bits) as required for AES-256.
   *
   * @param key the Base64-encoded key to validate
   * @param envVarName the environment variable name (for error messages)
   * @throws IllegalStateException if the key is invalid
   */
  private void validateKey(String key, String envVarName) {
    try {
      byte[] keyBytes = java.util.Base64.getDecoder().decode(key);

      if (keyBytes.length != 32) {
        String errorMsg =
            String.format(
                "Encryption key from %s has invalid length: %d bytes. Expected 32 bytes (256 bits) for AES-256.",
                envVarName, keyBytes.length);
        log.error(errorMsg);
        throw new IllegalStateException(errorMsg);
      }

    } catch (IllegalArgumentException e) {
      String errorMsg =
          String.format(
              "Encryption key from %s is not valid Base64. Please provide a Base64-encoded 256-bit key.",
              envVarName);
      log.error(errorMsg);
      throw new IllegalStateException(errorMsg, e);
    }
  }

  /**
   * Returns true if encryption is enabled (both keys are configured).
   *
   * <p>This method always returns true since the constructor throws an exception if keys are not
   * configured. It's provided for API completeness and potential future use.
   *
   * @return true if encryption is enabled
   */
  public boolean isEncryptionEnabled() {
    return encryptionKey != null
        && !encryptionKey.isEmpty()
        && backendStorageEncryptionKey != null
        && !backendStorageEncryptionKey.isEmpty();
  }
}
