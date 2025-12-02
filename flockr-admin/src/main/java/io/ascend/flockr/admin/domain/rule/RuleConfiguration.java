package io.ascend.flockr.admin.domain.rule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for rule configurations.
 *
 * <p>This interface serves as the polymorphic base for different rule configuration types. The
 * Jackson annotations enable automatic serialization/deserialization based on the {@code
 * configuration_type} property in the JSON.
 *
 * <p>Implementations:
 *
 * <ul>
 *   <li>{@link StreamConfiguration} - For real-time event pattern matching rules
 *   <li>{@link BatchConfiguration} - For SQL-based batch processing rules
 * </ul>
 *
 * @param <T> the type of source information, extending {@link SourceInfo}
 * @author Prithu Sharma
 * @since 1.0
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "configuration_type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = StreamConfiguration.class, name = "STREAM"),
  @JsonSubTypes.Type(value = BatchConfiguration.class, name = "BATCH")
})
public interface RuleConfiguration<T extends SourceInfo> {}
