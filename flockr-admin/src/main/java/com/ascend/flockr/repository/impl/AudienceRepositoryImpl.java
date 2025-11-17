package com.ascend.flockr.repository.impl;

import com.ascend.flockr.client.postgres.PostgresReaderClient;
import com.ascend.flockr.client.postgres.PostgresWriterClient;
import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.io.response.AudienceMetaResponse;
import com.ascend.flockr.repository.AudienceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of {@link AudienceRepository} using PostgreSQL as the data store.
 *
 * <p>This implementation handles:
 *
 * <ul>
 *   <li>Creating audiences with JSONB field serialization
 *   <li>Retrieving audience details with proper JSON deserialization
 *   <li>Optimized paginated queries with full-text search support
 * </ul>
 *
 * @author Flockr Team
 * @since 1.0
 */
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AudienceRepositoryImpl implements AudienceRepository {
  private final PostgresReaderClient postgresReaderClient;
  private final PostgresWriterClient postgresWriterClient;
  private final ObjectMapper objectMapper;

  private static final String SQL_CREATE_AUDIENCE =
      "INSERT INTO audiences (tenant_id, project_id, name, description, sinks, custom_audience_config, type, expiry_date, created_by) "
          + "VALUES ($1, $2, $3, $4, $5, CAST($6 AS JSONB), $7, to_timestamp($8), $9) RETURNING id";

  private static final String SQL_GET_AUDIENCE_BY_ID =
      "SELECT id, tenant_id, project_id, name, description, "
          + "EXTRACT(EPOCH FROM created_at)::BIGINT AS created_at, "
          + "EXTRACT(EPOCH FROM updated_at)::BIGINT AS updated_at, "
          + "EXTRACT(EPOCH FROM last_audience_updated_at)::BIGINT AS last_audience_updated_at, "
          + "user_count, custom_audience_config, type, verified, "
          + "EXTRACT(EPOCH FROM expiry_date)::BIGINT AS expiry_date, sinks, created_by "
          + "FROM audiences WHERE id = $1 AND tenant_id = $2 AND project_id = $3";

  /**
   * {@inheritDoc}
   *
   * <p>This implementation serializes JSON fields (custom_audience_config, sinks) and handles
   * timestamp conversion for expiry dates.
   */
  @Override
  public Single<Long> createAudience(AudienceMeta audienceMeta) {
    Tuple params =
        Tuple.tuple()
            .addValue(audienceMeta.getTenantId())
            .addValue(audienceMeta.getProjectId())
            .addValue(audienceMeta.getName())
            .addValue(audienceMeta.getDescription())
            .addValue(audienceMeta.getSinks())
            .addValue(audienceMeta.getCustomAudienceConfig())
            .addValue(audienceMeta.getType())
            .addValue(audienceMeta.getExpireDate())
            .addValue(audienceMeta.getCreatedBy());

    return postgresWriterClient
        .executeWithTransaction(
            conn ->
                postgresWriterClient
                    .executeAndGenerateId(conn, SQL_CREATE_AUDIENCE, params)
                    .toMaybe())
        .toSingle();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation deserializes JSON fields and converts database timestamps to epoch
   * milliseconds.
   */
  @Override
  public Single<AudienceMeta> getAudienceById(String tenantId, String projectId, Long id) {
    return postgresReaderClient.fetchOne(
        SQL_GET_AUDIENCE_BY_ID,
        Tuple.of(id, tenantId, projectId),
        row -> {
          AudienceMeta.AudienceMetaBuilder builder =
              AudienceMeta.builder()
                  .audienceId(row.getLong("id"))
                  .tenantId(row.getString("tenant_id"))
                  .projectId(row.getString("project_id"))
                  .name(row.getString("name"))
                  .description(row.getString("description"))
                  .type(row.getString("type"))
                  .verified(row.getBoolean("verified"))
                  .userCount(row.getLong("user_count"))
                  .expireDate(row.getLong("expiry_date"))
                  .lastAudienceUpdatedAt(row.getLong("last_audience_updated_at"))
                  .createdAt(row.getLong("created_at"))
                  .updatedAt(row.getLong("updated_at"))
                  .createdBy(row.getString("created_by"));

          // Map JSON fields
          String configJson = row.getString("custom_audience_config");
          if (configJson != null) {
            try {
              JsonNode configNode = objectMapper.readTree(configJson);
              builder.customAudienceConfig(configNode);
            } catch (Exception e) {
              throw new RuntimeException(
                  "Failed to deserialize custom_audience_config for audience id " + id, e);
            }
          }

          String sinksJson = row.getString("sinks");
          if (sinksJson != null && !sinksJson.isBlank()) {
            try {
              List<Long> sinks = objectMapper.readValue(sinksJson, new TypeReference<>() {});
              builder.sinks(sinks);
            } catch (Exception e) {
              throw new RuntimeException(
                  "Failed to deserialize sinks for audience id " + id + ": " + sinksJson, e);
            }
          }

          return builder.build();
        });
  }

  /**
   * Retrieves a paginated list of audiences with basic metadata and associated rule counts.
   *
   * <p>This method performs an optimized query using a Common Table Expression (CTE) to filter and
   * paginate audiences before joining with the rules table, ensuring consistent performance even
   * with large datasets and deep pagination offsets.
   *
   * <p><strong>Query Strategy:</strong>
   *
   * <ol>
   *   <li><strong>CTE (filtered_audiences):</strong> Applies filters, sorting, and pagination to
   *       the audiences table first, limiting the result set before joining
   *   <li><strong>LEFT JOIN:</strong> Joins the filtered audience subset with the rules table to
   *       count associated rules
   *   <li><strong>Aggregation:</strong> Groups by audience ID and counts rules per audience
   * </ol>
   *
   * <p><strong>Full-Text Search (FTS):</strong>
   *
   * <p>When {@code nameSearch} is provided, the query utilizes PostgreSQL's full-text search
   * capabilities:
   *
   * <ul>
   *   <li>Searches against the pre-indexed {@code name_vector} column (includes name)
   *   <li>Uses {@code plainto_tsquery} for stemming and language-aware parsing
   *   <li>Calculates relevance scores using {@code ts_rank()}
   *   <li>Results are sorted by FTS rank (most relevant first), then by creation date
   *   <li>Leverages GIN index {@code idx_audiences_name_vector} for fast lookups
   * </ul>
   *
   * <p><strong>Sorting Behavior:</strong>
   *
   * <ul>
   *   <li><strong>With nameSearch:</strong> ORDER BY fts_rank DESC, created_at DESC
   *   <li><strong>Without nameSearch:</strong> ORDER BY created_at DESC (newest first)
   * </ul>
   *
   * <p><strong>Performance Characteristics:</strong>
   *
   * <ul>
   *   <li>Time complexity: O(log n) for filtering + O(m) for counting rules, where n = total
   *       audiences, m = rules for filtered audiences
   *   <li>Scales efficiently to millions of audiences due to CTE-based pagination
   *   <li>Deep pagination (large offsets) maintains consistent performance
   *   <li>Single database round trip minimizes network latency
   * </ul>
   *
   * <p><strong>Index Dependencies:</strong>
   *
   * <ul>
   *   <li>{@code idx_audiences_name_vector} - GIN index for full-text search
   *   <li>{@code idx_rules_audience_id} - B-tree index for efficient rule counting
   *   <li>Primary key index on {@code audiences.id} for grouping
   * </ul>
   *
   * <p><strong>Example Usage:</strong>
   *
   * <pre>{@code
   * // Get first 20 audiences for tenant/project, sorted by creation date
   * repository.getAudiencesList("tenant1", "project1", null, null, null, 20, 0);
   *
   * // Search for "marketing" audiences, get top 10 by relevance
   * repository.getAudiencesList("tenant1", "project1", "marketing", null, null, 10, 0);
   *
   * // Get verified audiences created by specific user
   * repository.getAudiencesList("tenant1", "project1", null, "john.doe", true, 50, 0);
   *
   * // Combined: search + filters + deep pagination
   * repository.getAudiencesList("tenant1", "project1", "campaign", "admin", true, 20, 100);
   * }</pre>
   *
   * @param tenantId the tenant identifier from the request header
   * @param projectId the project identifier from the request header
   * @param nameSearch optional search term for full-text search against audience names and
   *     descriptions. If provided, results are ranked by relevance using {@code ts_rank()}.
   *     Supports stemming (e.g., "running" matches "run") and multi-word queries. Pass {@code null}
   *     or blank to skip FTS filtering.
   * @param createdBy optional exact-match filter for the creator's username. Pass {@code null} or
   *     blank to skip this filter.
   * @param verified optional filter for audience verification status. Pass {@code true} for
   *     verified only, {@code false} for unverified only, or {@code null} to include all audiences.
   * @param limit maximum number of results to return. Must be greater than 0. Typical values:
   *     10-100. This parameter is never null as enforced by the API layer.
   * @param offset number of results to skip for pagination (0-based). Use with {@code limit} to
   *     implement page-based navigation. This parameter is never null as enforced by the API layer.
   * @return a {@link Single} emitting a list of {@link AudienceMetaResponse} objects containing
   *     audience metadata with computed rule counts. The list is ordered according to the sorting
   *     rules described above. Returns an empty list if no audiences match the criteria.
   * @throws RuntimeException if database access fails or query execution encounters an error
   * @see AudienceMetaResponse for the structure of returned data
   * @see io.reactivex.rxjava3.core.Single for reactive stream semantics
   */
  @Override
  public Single<List<AudienceMetaResponse>> getAudiencesList(
      String tenantId,
      String projectId,
      String nameSearch,
      String createdBy,
      Boolean verified,
      Integer limit,
      Integer offset) {

    StringBuilder sql = new StringBuilder();
    int paramIndex = 1;

    sql.append("WITH filtered_audiences AS ( ");
    sql.append("  SELECT a.id, a.name, a.description, a.type, a.verified, a.user_count, ");
    sql.append("  a.created_by, EXTRACT(EPOCH FROM a.created_at)::BIGINT AS created_at, ");
    sql.append("  EXTRACT(EPOCH FROM a.updated_at)::BIGINT AS updated_at, ");
    sql.append("  EXTRACT(EPOCH FROM a.expire_date)::BIGINT AS expire_date");

    List<Object> params = new ArrayList<>();

    if (nameSearch != null && !nameSearch.isBlank()) {
      sql.append(", ts_rank(a.name_vector, plainto_tsquery('english', $")
          .append(paramIndex++)
          .append(")) AS fts_rank ");
      params.add(nameSearch); // for ts_rank in SELECT
    }

    sql.append("  FROM audiences a ");
    sql.append("  WHERE a.tenant_id = $")
        .append(paramIndex++)
        .append(" AND a.project_id = $")
        .append(paramIndex++)
        .append(" ");
    params.add(tenantId);
    params.add(projectId);

    // Add filters
    if (nameSearch != null && !nameSearch.isBlank()) {
      sql.append("  AND a.name_vector @@ plainto_tsquery('english', $")
          .append(paramIndex++)
          .append(") ");
      params.add(nameSearch);
    }

    if (createdBy != null && !createdBy.isBlank()) {
      sql.append("  AND a.created_by = $").append(paramIndex++).append(" ");
      params.add(createdBy);
    }

    if (verified != null) {
      sql.append("  AND a.verified = $").append(paramIndex++).append(" ");
      params.add(verified);
    }

    sql.append("  ORDER BY ");
    if (nameSearch != null && !nameSearch.isBlank()) {
      sql.append("fts_rank DESC, ");
    }
    sql.append("a.created_at DESC ");
    sql.append("  LIMIT $")
        .append(paramIndex++)
        .append(" OFFSET $")
        .append(paramIndex++)
        .append(" ");
    params.add(limit);
    params.add(offset);
    sql.append(") ");

    // Now JOIN only the filtered/paginated results
    sql.append("SELECT fa.*, COALESCE(COUNT(r.id), 0) AS rule_count ");
    sql.append("FROM filtered_audiences fa ");
    sql.append("LEFT JOIN rules r ON r.audience_id = fa.id ");
    sql.append("GROUP BY fa.id, fa.name, fa.description, fa.type, fa.verified, ");
    sql.append("fa.user_count, fa.created_by, fa.created_at, fa.updated_at, fa.expire_date");

    if (nameSearch != null && !nameSearch.isBlank()) {
      sql.append(", fa.fts_rank ");
      sql.append("ORDER BY fa.fts_rank DESC, fa.created_at DESC");
    } else {
      sql.append(" ORDER BY fa.created_at DESC");
    }

    Tuple tuple = Tuple.tuple();
    for (Object param : params) {
      tuple.addValue(param);
    }

    return postgresReaderClient.fetchAll(
        sql.toString(),
        tuple,
        row ->
            AudienceMetaResponse.builder()
                .audienceId(row.getLong("id"))
                .name(row.getString("name"))
                .description(row.getString("description"))
                .type(row.getString("type"))
                .verified(row.getBoolean("verified"))
                .userCount(row.getLong("user_count"))
                .ruleCount(row.getLong("rule_count"))
                .expireDate(row.getLong("expire_date"))
                .createdAt(row.getLong("created_at"))
                .updatedAt(row.getLong("updated_at"))
                .createdBy(row.getString("created_by"))
                .build());
  }
}
