package io.ascend.flockr.admin.repository.impl;

import com.google.inject.Inject;
import io.ascend.flockr.admin.client.postgres.PostgresWriterClient;
import io.ascend.flockr.admin.repository.ExecutionSync;
import io.ascend.flockr.admin.util.CommonUtil;
import io.reactivex.rxjava3.core.Maybe;
import io.vertx.rxjava3.sqlclient.Tuple;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL implementation of distributed lease-based execution synchronization.
 *
 * <p>Uses PostgreSQL's atomic INSERT ... ON CONFLICT for acquiring leases with TTL.
 *
 * @author Prithu Sharma
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PostgresExecutionSync implements ExecutionSync {

  private static final String INSTANCE_ID = CommonUtil.getHostAddress();

  /**
   * SQL to atomically acquire or renew a lease. Uses make_interval() for proper interval handling
   * with prepared statement parameters.
   */
  private static final String ACQUIRE_LEASE_SQL =
      """
      INSERT INTO distributed_lease (lease_key, holder_id, acquired_at, expires_at)
      VALUES ($1, $2, NOW(), NOW() + make_interval(secs => $3))
      ON CONFLICT (lease_key) DO UPDATE
      SET holder_id = EXCLUDED.holder_id,
          acquired_at = NOW(),
          expires_at = NOW() + make_interval(secs => $3)
      WHERE distributed_lease.expires_at < NOW()
      RETURNING expires_at
      """;

  private final PostgresWriterClient postgresWriterClient;

  @Override
  public Maybe<Boolean> acquire(String key, Duration ttl) {
    long seconds = ttl.toSeconds();

    return postgresWriterClient
        .getConnection()
        .flatMapMaybe(
            conn ->
                conn.preparedQuery(ACQUIRE_LEASE_SQL)
                    .rxExecute(Tuple.of(key, INSTANCE_ID, seconds))
                    .flatMapMaybe(
                        rows -> {
                          if (rows.rowCount() == 0) {
                            log.debug("Lease {} not available (TTL not expired)", key);
                            return Maybe.empty();
                          }
                          var expiresAt = rows.iterator().next().getLocalDateTime("expires_at");
                          log.debug(
                              "Acquired lease {} with TTL={}s, expires_at={}",
                              key,
                              seconds,
                              expiresAt);
                          return Maybe.just(true);
                        })
                    .doFinally(conn::close));
  }
}
