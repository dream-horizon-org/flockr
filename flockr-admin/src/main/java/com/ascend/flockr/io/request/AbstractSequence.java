package com.ascend.flockr.io.request;

import com.ascend.flockr.model.query.EnrichableFields;
import io.reactivex.Completable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractSequence {

  private List<? extends AbstractDestination> destinations;

  protected abstract Completable validateAndUpdate(
      EnrichableFields enrichableFields, String ruleName);
}
