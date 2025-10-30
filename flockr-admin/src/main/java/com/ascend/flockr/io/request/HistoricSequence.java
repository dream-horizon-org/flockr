package com.ascend.flockr.io.request;

import com.ascend.flockr.annotation.Enumeration;
import com.ascend.flockr.io.StoredDataSourceType;
import com.ascend.flockr.model.query.DestinationLimits;
import com.ascend.flockr.model.query.EnrichableFields;
import com.ascend.flockr.model.task.constant.DestinationType;
import com.ascend.flockr.util.Constant;
import com.ascend.flockr.util.rule.HistoricTaskRequestSourceVisitor;
import com.ascend.flockr.util.rule.HistoricTaskRequestVisitableSource;
import com.ascend.flockr.util.sqlparser.SQLStatementUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.reactivex.Completable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.*;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.statement.select.PlainSelect;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricSequence extends AbstractSequence {

  @NotNull @Valid private DataSource source;

  @Override
  protected Completable validateAndUpdate(EnrichableFields enrichableFields, String ruleName) {
    DestinationLimits destinationLimits = enrichableFields.getDestinationLimits();
    List<String> destinations = new ArrayList<>();
    this.getDestinations().forEach(dest -> destinations.add(dest.getDestinationType().name()));

    return SQLStatementUtil.validateSingleColumnQueryAndGetSelectQuery(
            this.source.getQuery(), Constant.COHORT_QUERY_USER_ID)
        .filter(
            plainSelect ->
                !(this.getDestinations().size() == 1
                    && this.getDestinations()
                        .get(0)
                        .getDestinationType()
                        .equals(DestinationType.Iceberg)))
        .flatMapCompletable(
            select -> {
              PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
              if (plainSelect.getLimit() == null) {
                Long minLimit = destinationLimits.getMinLimit(destinations);
                SQLStatementUtil.addLimit(plainSelect, minLimit);
                this.getSource().setQuery(select.toString());
              } else {
                destinationLimits.checkLimitWithinBounds(
                    destinations,
                    plainSelect.getLimit().getRowCount(LongValue.class).getValue(),
                    Optional.of(ruleName));
              }
              return Completable.complete();
            });
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXISTING_PROPERTY,
      property = "type",
      visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = AthenaDataSource.class, name = "ATHENA"),
    @JsonSubTypes.Type(value = RedShiftDataSource.class, name = "REDSHIFT")
  })
  public abstract static class DataSource implements HistoricTaskRequestVisitableSource {

    @NotEmpty
    @Enumeration(enumClass = StoredDataSourceType.class)
    private String type;

    @NotEmpty private String query;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RedShiftDataSource extends DataSource {

    @Override
    public void accept(HistoricTaskRequestSourceVisitor visitor) {
      visitor.visit(this);
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AthenaDataSource extends DataSource {

    @Override
    public void accept(HistoricTaskRequestSourceVisitor visitor) {
      visitor.visit(this);
    }
  }
}
