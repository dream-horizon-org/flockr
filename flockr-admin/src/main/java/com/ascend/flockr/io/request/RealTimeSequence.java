package com.ascend.flockr.io.request;

import com.ascend.flockr.annotation.AcceptedValues;
import com.ascend.flockr.annotation.Enumeration;
import com.ascend.flockr.exception.DefinedException;
import com.ascend.flockr.exception.ErrorEntity;
import com.ascend.flockr.model.query.EnrichableFields;
import com.ascend.flockr.model.task.constant.Contiguity;
import com.ascend.flockr.model.task.constant.PropertyType;
import com.ascend.flockr.model.task.constant.TimeUnit;
import com.ascend.flockr.util.rule.RealtimeRequestOperatorVisitor;
import com.ascend.flockr.util.rule.RealtimeRequestVisitableOperator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.reactivex.Completable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RealTimeSequence extends AbstractSequence {

  @NotEmpty private List<String> groupBy;
  @NotEmpty @Valid private List<PatternElement> pattern;
  private CohortFilter cohortFilter;
  @Valid private RealTimeSequence.Constraint constraint;

  @Override
  protected Completable validateAndUpdate(EnrichableFields enrichableFields, String ruleName) {
    validateUserIdInGroupBy();
    validateConstraintForMoreThanOneEvent();
    // validateUserIdInOutputAttribute();
    return Completable.complete();
  }

  protected void validateUserIdInGroupBy() {
    if (!this.groupBy.contains("userId")) {
      throw new DefinedException(ErrorEntity.INVALID_GROUP_BY);
    }
  }

  protected void validateConstraintForMoreThanOneEvent() {
    if (this.pattern.size() > 1 && this.constraint == null) {
      throw new DefinedException(ErrorEntity.CONSTRAINTS_CANNOT_BE_NULL);
    }
  }

  /*
   protected void validateUserIdInOutputAttribute() {
     for (AbstractDestination destination : this.getDestinations()) {
       if (destination instanceof CleverTapDestinationStruct) {
         boolean userIdPresent = false;
         for (CleverTapProperty property :
             ((CleverTapDestinationStruct) destination).getConfig().getProperties()) {
           if (property.getPropertyName().equals("userId") && property.getAlias() == null) {
             userIdPresent = true;
             break;
           }
         }

         if (!userIdPresent) {
           throw new DefinedException(ErrorEntity.USER_ID_NOT_FOUND_IN_CT_DESTINATION_PROPERTIES);
         }

         break;
       }
     }
   }
  */

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PatternElement {

    @Min(1)
    @NotNull
    private Integer order;

    @NotNull @Valid private RealTimeSequence.PatternData data;

    @Enumeration(enumClass = Contiguity.class)
    private String contiguity;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CohortFilter {
    private List<String> belongsTo;
    private List<String> notBelongsTo;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Constraint {

    @NotNull
    @Enumeration(enumClass = Temporal.class)
    private String temporal;

    @NotNull
    @Enumeration(enumClass = TimeUnit.class)
    private String timeUnit;

    @NotNull private Long value;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PatternData {

    @Valid RealTimeSequence.Quantifier quantifier;
    @NotEmpty @Valid List<DataHighWayEvent> event;
  }

  public enum Temporal {
    within
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Quantifier {

    @NotNull
    @Min(1)
    private Integer conditionValue;

    @NotNull
    @AcceptedValues(values = {"=", ">="})
    private String conditionOperator;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DataHighWayEvent {

    @NotNull private Integer eventId;
    @NotNull private String eventName;
    @NotNull private String kafkaTopic;
    @NotNull private String org;
    @NotNull private String eventSource;
    @NotEmpty private List<String> platform;
    @Valid private List<PropertyFilter> condition;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXISTING_PROPERTY,
      property = "conditionOperator",
      visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = PropertyFilter.RangeOperator.class, name = "><"),
    @JsonSubTypes.Type(value = PropertyFilter.ListOperator.class, name = "in"),
    @JsonSubTypes.Type(value = PropertyFilter.ListOperator.class, name = "not_in"),
    @JsonSubTypes.Type(value = PropertyFilter.ListOperator.class, name = "contains"),
    @JsonSubTypes.Type(value = PropertyFilter.ListOperator.class, name = "not_contains"),
    @JsonSubTypes.Type(value = PropertyFilter.SingleValueOperator.class, name = "="),
    @JsonSubTypes.Type(value = PropertyFilter.SingleValueOperator.class, name = "!="),
    @JsonSubTypes.Type(value = PropertyFilter.SingleValueOperator.class, name = "<"),
    @JsonSubTypes.Type(value = PropertyFilter.SingleValueOperator.class, name = ">"),
    @JsonSubTypes.Type(value = PropertyFilter.SingleValueOperator.class, name = ">="),
    @JsonSubTypes.Type(value = PropertyFilter.SingleValueOperator.class, name = "<=")
  })
  public abstract static class PropertyFilter implements RealtimeRequestVisitableOperator {

    private String filterType = "event";

    @NotEmpty private String propertyName;

    @NotNull
    @Enumeration(enumClass = PropertyType.class)
    private String propertyType;

    @NotNull
    @AcceptedValues(
        values = {
          "=",
          "!=",
          "<",
          ">",
          "<=",
          ">=",
          "><",
          "in",
          "not_in",
          "contains",
          "not_contains"
        })
    private String conditionOperator;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SingleValueOperator extends PropertyFilter {
      @NotEmpty private String conditionValue;

      @Override
      public void accept(RealtimeRequestOperatorVisitor visitor) {
        visitor.visit(this);
      }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListOperator extends PropertyFilter {
      @NotEmpty private List<String> conditionValues;

      @Override
      public void accept(RealtimeRequestOperatorVisitor visitor) {
        visitor.visit(this);
      }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RangeOperator extends PropertyFilter {
      @NotEmpty private String conditionStartValue;

      @NotEmpty private String conditionEndValue;

      @Override
      public void accept(RealtimeRequestOperatorVisitor visitor) {
        visitor.visit(this);
      }
    }
  }
}
