package io.ascend.flockr.admin.client.kafka;

import io.reactivex.rxjava3.core.Completable;

public interface KafkaProducerClient {

  Completable close();
}
