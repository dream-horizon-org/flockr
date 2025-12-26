package com.dream11.flocker.engine.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for JSON serialization and deserialization using Jackson.
 * 
 * <p>This class provides a centralized, thread-safe ObjectMapper instance
 * for JSON operations throughout the application. The ObjectMapper is configured
 * with default settings suitable for most use cases.
 * 
 * <p><b>Usage:</b>
 * <pre>{@code
 * String json = JsonMapper.getInstance().writeValueAsString(object);
 * MyObject obj = JsonMapper.getInstance().readValue(json, MyObject.class);
 * }</pre>
 * 
 * <p><b>Thread Safety:</b>
 * <p>The ObjectMapper instance is thread-safe and can be safely shared across
 * multiple threads.
 * 
 * @author Shivam-Raghuwanshi
 */
@Slf4j
public class JsonMapper {

    /** Singleton instance of ObjectMapper for JSON operations. */
    private static final ObjectMapper INSTANCE = new ObjectMapper();

    /**
     * Gets the singleton ObjectMapper instance.
     * 
     * @return The ObjectMapper instance.
     */
    public static ObjectMapper getInstance() {
        return INSTANCE;
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private JsonMapper() {
        // Utility class - prevent instantiation
    }
}

