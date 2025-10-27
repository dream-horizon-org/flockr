package com.ascend.flockr.service.flink;

import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.rxjava3.core.Vertx;

public class FlinkModule extends AbstractVerticle {

  public FlinkModule(Vertx vertx) {
    super();
  }

  //  @Override
  //  protected void bindConfiguration() {
  //    bind(FlinkClient.class).to(FlinkClientImpl.class);
  //    bind(new TypeLiteral<AsyncJobService<PatternSequenceRule>>() {})
  //        .to(EventStreamJobService.class);
  //  }
}
