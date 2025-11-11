package com.ascend.flockr.repository;

import io.reactivex.rxjava3.core.Single;

public interface HealthCheckDAO {
  Single<Boolean> isMySQLReaderConnected();

  Single<Boolean> isAerospikeConnected();

  Single<Boolean> isUnderMaintenance();
}
