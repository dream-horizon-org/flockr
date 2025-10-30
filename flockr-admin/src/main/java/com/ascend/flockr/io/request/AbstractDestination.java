package com.ascend.flockr.io.request;

import com.ascend.flockr.model.task.constant.DestinationType;
import com.ascend.flockr.util.rule.CohortRequestDestinationVisitor;
import com.ascend.flockr.util.rule.CohortRequestVisitableDestination;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "destinationType",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(
      value = AbstractDestination.CleverTapDestinationStruct.class,
      name = "CleverTap"),
  @JsonSubTypes.Type(
      value = AbstractDestination.UserCohortDestinationStruct.class,
      name = "UserCohortService"),
  @JsonSubTypes.Type(
      value = AbstractDestination.DataFeastDestinationStruct.class,
      name = "DataFeast"),
  @JsonSubTypes.Type(value = AbstractDestination.IcebergDestinationStruct.class, name = "Iceberg"),
  @JsonSubTypes.Type(
      value = AbstractDestination.ComaServiceDestinationStruct.class,
      name = "ComaService"),
  @JsonSubTypes.Type(
      value = AbstractDestination.UserCohortFanCodeDestinationStruct.class,
      name = "UserCohortServiceFanCode")
})
public abstract class AbstractDestination implements CohortRequestVisitableDestination {
  @NotNull private DestinationType destinationType;

  public AbstractDestination(DestinationType destinationType) {
    this.destinationType = destinationType;
  }

  public String eventName() {
    return null;
  }

  public String datadogTag() {
    return this.destinationType + ":true";
  }

  public abstract Integer order();

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CleverTapDestinationStruct extends AbstractDestination {

    public final Integer order = 5;

    @NotNull @Valid private AbstractDestination.CleverTapDestinationConfig config;

    public CleverTapDestinationStruct(String eventName) {
      super(DestinationType.CleverTap);
      this.config = new AbstractDestination.CleverTapDestinationConfig(eventName);
    }

    @Override
    public void accept(CohortRequestDestinationVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public String eventName() {
      return config.getEventName();
    }

    @Override
    public Integer order() {
      return this.order;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class UserCohortDestinationStruct extends AbstractDestination {

    public final Integer order = 2;

    public UserCohortDestinationStruct() {
      super(DestinationType.UserCohortService);
    }

    @Override
    public void accept(CohortRequestDestinationVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public Integer order() {
      return this.order;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class UserCohortFanCodeDestinationStruct extends AbstractDestination {

    public final Integer order = 3;

    public UserCohortFanCodeDestinationStruct() {
      super(DestinationType.UserCohortServiceFanCode);
    }

    @Override
    public void accept(CohortRequestDestinationVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public Integer order() {
      return this.order;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DataFeastDestinationStruct extends AbstractDestination {

    public final Integer order = 4;

    public DataFeastDestinationStruct() {
      super(DestinationType.DataFeast);
    }

    @Override
    public void accept(CohortRequestDestinationVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public Integer order() {
      return this.order;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class IcebergDestinationStruct extends AbstractDestination {

    public final Integer order = 1;

    public IcebergDestinationStruct() {
      super(DestinationType.Iceberg);
    }

    @Override
    public void accept(CohortRequestDestinationVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public Integer order() {
      return this.order;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CleverTapDestinationConfig {
    @NotEmpty private String eventName;

    //    @Valid
    //    @Null(groups = {HistoricTaskConstraint.class})
    //    @NotEmpty(groups = {RealTimeTaskConstraint.class})
    //    private List<CleverTapProperty> properties;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CleverTapProperty {
    @NotEmpty private String propertyName;
    @NotEmpty private String sourceEvent;
    private String alias;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ComaServiceDestinationStruct extends AbstractDestination {

    public final Integer order = Integer.MAX_VALUE;

    public ComaServiceDestinationStruct() {
      super(DestinationType.ComaService);
    }

    @Override
    public void accept(CohortRequestDestinationVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public Integer order() {
      return this.order;
    }
  }
}
