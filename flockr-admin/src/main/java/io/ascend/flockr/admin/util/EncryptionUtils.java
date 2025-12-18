package io.ascend.flockr.admin.util;

import com.google.inject.Inject;
import io.ascend.flockr.admin.config.EncryptionConfig;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/**
 * Comprehensive encryption utility for the Flockr application.
 *
 * <p>This utility provides:
 *
 * <ul>
 *   <li><b>AES-256-GCM Encryption:</b> Low-level encrypt/decrypt operations
 *   <li><b>Double-Layer Credential Encryption:</b> Secure handling of sensitive configuration
 *       fields
 * </ul>
 *
 * <h2>Double-Layer Encryption Strategy</h2>
 *
 * <p>This utility implements a two-layer encryption strategy for sensitive credentials:
 *
 * <ul>
 *   <li><strong>Layer 1 (Frontend):</strong> Frontend encrypts credentials with ENCRYPTION_KEY
 *       before transmission
 *   <li><strong>Layer 2 (Backend Storage):</strong> Backend decrypts Layer 1, then re-encrypts with
 *       BACKEND_STORAGE_ENCRYPTION_KEY before database storage
 * </ul>
 *
 * <h3>Processing Flows:</h3>
 *
 * <p><strong>Before Save (processForStorage):</strong>
 *
 * <ol>
 *   <li>Receive config with frontend-encrypted credentials
 *   <li>Decrypt sensitive fields using ENCRYPTION_KEY (Layer 1)
 *   <li>Re-encrypt sensitive fields using BACKEND_STORAGE_ENCRYPTION_KEY (Layer 2)
 *   <li>Store in database with backend-encrypted credentials
 * </ol>
 *
 * <p><strong>After Retrieval (processFromStorage):</strong>
 *
 * <ol>
 *   <li>Receive config with backend-encrypted credentials from database
 *   <li>Decrypt sensitive fields using BACKEND_STORAGE_ENCRYPTION_KEY (Layer 2)
 *   <li>Return plaintext credentials for application use
 * </ol>
 *
 * <h2>AES-256-GCM Encryption</h2>
 *
 * <p>The utility uses AES-256-GCM for encryption with the following features:
 *
 * <ul>
 *   <li>256-bit keys for strong security
 *   <li>96-bit (12-byte) random IV for each encryption operation
 *   <li>128-bit authentication tag for integrity verification
 *   <li>Base64 encoding for safe storage and transmission
 *   <li>Output format: {@code IV:ENCRYPTED_DATA} (both Base64-encoded)
 * </ul>
 *
 * <h2>Sensitive Field Detection</h2>
 *
 * <p>Fields are automatically identified as sensitive based on naming patterns (case-insensitive):
 *
 * <ul>
 *   <li>Exact matches: accessKey, secretKey, password, apiKey, token
 *   <li>Snake_case variants: access_key, secret_key, api_key, etc.
 *   <li>Compound names: secretAccessKey, authToken, bearerToken
 *   <li>Suffix matches: fields ending with Key, Secret, Token, Password, Pwd
 * </ul>
 *
 * <h2>Key Generation</h2>
 *
 * <p>Encryption keys should be generated using the bash script utility:
 *
 * <pre>
 * ./docker/generate-encryption-keys.sh
 * </pre>
 *
 * <p><strong>Note:</strong> This class uses Guice static injection. Ensure {@code ServiceModule}
 * includes static injection for this class.
 *
 * @author Flockr Team
 * @since 1.0
 */
@Slf4j
public final class EncryptionUtils {

  // ==================== Constants ====================

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12; // 96 bits
  private static final int GCM_TAG_LENGTH = 128; // 128 bits
  private static final String DELIMITER = ":";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /** Set of field name patterns that identify sensitive credential fields. */
  private static final Set<String> SENSITIVE_FIELD_PATTERNS = new HashSet<>();

  static {
    // Exact matches (case-insensitive)
    SENSITIVE_FIELD_PATTERNS.add("accesskey");
    SENSITIVE_FIELD_PATTERNS.add("access_key");
    SENSITIVE_FIELD_PATTERNS.add("secretkey");
    SENSITIVE_FIELD_PATTERNS.add("secret_key");
    SENSITIVE_FIELD_PATTERNS.add("secretaccesskey");
    SENSITIVE_FIELD_PATTERNS.add("secret_access_key");
    SENSITIVE_FIELD_PATTERNS.add("password");
    SENSITIVE_FIELD_PATTERNS.add("pwd");
    SENSITIVE_FIELD_PATTERNS.add("apikey");
    SENSITIVE_FIELD_PATTERNS.add("api_key");
    SENSITIVE_FIELD_PATTERNS.add("token");
    SENSITIVE_FIELD_PATTERNS.add("authtoken");
    SENSITIVE_FIELD_PATTERNS.add("auth_token");
    SENSITIVE_FIELD_PATTERNS.add("bearertoken");
    SENSITIVE_FIELD_PATTERNS.add("bearer_token");
  }

  @Inject private static EncryptionConfig encryptionConfig;

  /**
   * Private constructor to prevent instantiation.
   *
   * @throws UnsupportedOperationException always thrown when constructor is invoked
   */
  private EncryptionUtils() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  // ==================== Low-Level Encryption Methods ====================

  /**
   * Encrypts plaintext using AES-256-GCM with the provided Base64-encoded key.
   *
   * <p>The method generates a random IV for each encryption operation, ensuring that the same
   * plaintext encrypted multiple times produces different ciphertexts.
   *
   * <p>Output format: {@code Base64(IV):Base64(EncryptedData)}
   *
   * @param plaintext the plaintext to encrypt
   * @param base64Key the Base64-encoded 256-bit (32-byte) encryption key
   * @return the encrypted text in format "IV:ENCRYPTED_DATA", both Base64-encoded
   * @throws RuntimeException if encryption fails
   */
  public static String encrypt(String plaintext, String base64Key) {
    try {
      // Decode the Base64-encoded key
      byte[] keyBytes = Base64.getDecoder().decode(base64Key);
      SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

      // Generate random IV
      byte[] iv = new byte[GCM_IV_LENGTH];
      SECURE_RANDOM.nextBytes(iv);

      // Initialize cipher
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

      // Encrypt the plaintext
      byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      // Encode IV and encrypted data as Base64 and combine with delimiter
      String ivBase64 = Base64.getEncoder().encodeToString(iv);
      String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);

      return ivBase64 + DELIMITER + encryptedBase64;

    } catch (Exception e) {
      log.error("Encryption failed: {}", e.getMessage());
      throw new RuntimeException("Failed to encrypt data", e);
    }
  }

  /**
   * Decrypts ciphertext using AES-256-GCM with the provided Base64-encoded key.
   *
   * <p>Expected input format: {@code Base64(IV):Base64(EncryptedData)}
   *
   * @param encryptedText the encrypted text in format "IV:ENCRYPTED_DATA"
   * @param base64Key the Base64-encoded 256-bit (32-byte) decryption key
   * @return the decrypted plaintext
   * @throws RuntimeException if decryption fails or input format is invalid
   */
  public static String decrypt(String encryptedText, String base64Key) {
    try {
      // Split IV and encrypted data
      String[] parts = encryptedText.split(DELIMITER, 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException(
            "Invalid encrypted text format. Expected 'IV:ENCRYPTED_DATA'");
      }

      // Decode Base64 components
      byte[] iv = Base64.getDecoder().decode(parts[0]);
      byte[] encryptedBytes = Base64.getDecoder().decode(parts[1]);

      // Decode the Base64-encoded key
      byte[] keyBytes = Base64.getDecoder().decode(base64Key);
      SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

      // Initialize cipher
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

      // Decrypt the data
      byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

      return new String(decryptedBytes, StandardCharsets.UTF_8);

    } catch (Exception e) {
      log.error("Decryption failed: {}", e.getMessage());
      throw new RuntimeException("Failed to decrypt data", e);
    }
  }

  /**
   * Checks if a string appears to be encrypted (contains the IV:DATA delimiter pattern).
   *
   * <p>This is a simple heuristic check that verifies the string contains a colon delimiter and
   * both parts appear to be Base64-encoded.
   *
   * @param value the string to check
   * @return true if the string appears to be encrypted, false otherwise
   */
  public static boolean isEncrypted(String value) {
    if (value == null || value.isEmpty()) {
      return false;
    }

    String[] parts = value.split(DELIMITER, 2);
    if (parts.length != 2) {
      return false;
    }

    // Check if both parts look like Base64 (alphanumeric + / + = characters)
    return parts[0].matches("^[A-Za-z0-9+/=]+$") && parts[1].matches("^[A-Za-z0-9+/=]+$");
  }

  // ==================== Double-Layer Credential Encryption Methods ====================

  /**
   * Processes a configuration JsonObject before storing in the database.
   *
   * <p>Decrypts Layer 1 (frontend encryption) and re-encrypts with Layer 2 (backend storage
   * encryption).
   *
   * @param config the configuration JsonObject with frontend-encrypted credentials
   * @return a new JsonObject with backend-encrypted credentials ready for database storage
   */
  public static JsonObject processForStorage(JsonObject config) {
    if (config == null || config.isEmpty()) {
      return config;
    }

    log.debug("Processing config for storage (Layer 1 decrypt -> Layer 2 encrypt)");
    JsonObject processed = new JsonObject();

    for (String key : config.fieldNames()) {
      Object value = config.getValue(key);

      if (value == null) {
        processed.putNull(key);
      } else if (isSensitiveField(key) && value instanceof String stringValue) {
        try {
          // Decrypt Layer 1 (frontend encryption)
          String decrypted = decryptIfEncrypted(stringValue, encryptionConfig.getEncryptionKey());

          // Encrypt Layer 2 (backend storage)
          String encrypted = encrypt(decrypted, encryptionConfig.getBackendStorageEncryptionKey());

          processed.put(key, encrypted);
          log.debug("Processed sensitive field '{}' for storage", key);

        } catch (Exception e) {
          log.error("Failed to process sensitive field '{}' for storage: {}", key, e.getMessage());
          throw new RuntimeException(
              "Failed to process sensitive field '" + key + "' for storage", e);
        }
      } else if (value instanceof JsonObject) {
        // Recursively process nested objects
        processed.put(key, processForStorage((JsonObject) value));
      } else if (value instanceof JsonArray) {
        // Process arrays that might contain objects
        processed.put(key, processArrayForStorage((JsonArray) value));
      } else {
        // Non-sensitive field, copy as-is
        processed.put(key, value);
      }
    }

    return processed;
  }

  /**
   * Processes a configuration JsonObject after retrieving from the database.
   *
   * <p>Decrypts Layer 2 (backend storage encryption) to return plaintext credentials.
   *
   * @param config the configuration JsonObject with backend-encrypted credentials from database
   * @return a new JsonObject with plaintext credentials ready for application use
   */
  public static JsonObject processFromStorage(JsonObject config) {
    if (config == null || config.isEmpty()) {
      return config;
    }

    log.debug("Processing config from storage (Layer 2 decrypt -> plaintext)");
    JsonObject processed = new JsonObject();

    for (String key : config.fieldNames()) {
      Object value = config.getValue(key);

      if (value == null) {
        processed.putNull(key);
      } else if (isSensitiveField(key) && value instanceof String) {
        String stringValue = (String) value;
        try {
          // Decrypt Layer 2 (backend storage) to plaintext
          String decrypted =
              decryptIfEncrypted(stringValue, encryptionConfig.getBackendStorageEncryptionKey());

          processed.put(key, decrypted);
          log.debug("Decrypted sensitive field '{}' from storage", key);

        } catch (Exception e) {
          log.error("Failed to decrypt sensitive field '{}' from storage: {}", key, e.getMessage());
          // Return the original value if decryption fails (might be legacy unencrypted data)
          processed.put(key, stringValue);
          log.warn(
              "Returning original value for field '{}' - might be legacy unencrypted data", key);
        }
      } else if (value instanceof JsonObject) {
        // Recursively process nested objects
        processed.put(key, processFromStorage((JsonObject) value));
      } else if (value instanceof JsonArray) {
        // Process arrays that might contain objects
        processed.put(key, processArrayFromStorage((JsonArray) value));
      } else {
        // Non-sensitive field, copy as-is
        processed.put(key, value);
      }
    }

    return processed;
  }

  /**
   * Processes a JsonArray for storage, recursively handling nested objects.
   *
   * @param array the JsonArray to process
   * @return a new JsonArray with processed elements
   */
  private static JsonArray processArrayForStorage(JsonArray array) {
    JsonArray processed = new JsonArray();

    for (int i = 0; i < array.size(); i++) {
      Object value = array.getValue(i);

      if (value instanceof JsonObject) {
        processed.add(processForStorage((JsonObject) value));
      } else if (value instanceof JsonArray) {
        processed.add(processArrayForStorage((JsonArray) value));
      } else {
        processed.add(value);
      }
    }

    return processed;
  }

  /**
   * Processes a JsonArray from storage, recursively handling nested objects.
   *
   * @param array the JsonArray to process
   * @return a new JsonArray with processed elements
   */
  private static JsonArray processArrayFromStorage(JsonArray array) {
    JsonArray processed = new JsonArray();

    for (int i = 0; i < array.size(); i++) {
      Object value = array.getValue(i);

      if (value instanceof JsonObject) {
        processed.add(processFromStorage((JsonObject) value));
      } else if (value instanceof JsonArray) {
        processed.add(processArrayFromStorage((JsonArray) value));
      } else {
        processed.add(value);
      }
    }

    return processed;
  }

  /**
   * Determines if a field name indicates it contains sensitive credential data.
   *
   * @param fieldName the field name to check
   * @return true if the field should be treated as sensitive
   */
  private static boolean isSensitiveField(String fieldName) {
    if (fieldName == null || fieldName.isEmpty()) {
      return false;
    }

    String lowerFieldName = fieldName.toLowerCase();

    // Check exact matches
    if (SENSITIVE_FIELD_PATTERNS.contains(lowerFieldName)) {
      return true;
    }

    // Check suffix patterns
    return lowerFieldName.endsWith("key")
        || lowerFieldName.endsWith("secret")
        || lowerFieldName.endsWith("token")
        || lowerFieldName.endsWith("password")
        || lowerFieldName.endsWith("pwd");
  }

  /**
   * Decrypts a value if it appears to be encrypted, otherwise returns it as-is.
   *
   * <p>This handles backward compatibility with legacy unencrypted data.
   *
   * @param value the value to potentially decrypt
   * @param key the encryption key to use
   * @return the decrypted value or original value if not encrypted
   */
  private static String decryptIfEncrypted(String value, String key) {
    if (value == null || value.isEmpty()) {
      return value;
    }

    // Check if the value looks encrypted (has IV:DATA format)
    if (isEncrypted(value)) {
      return decrypt(value, key);
    }

    // Not encrypted, return as-is (backward compatibility for legacy data)
    return value;
  }
}
