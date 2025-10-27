package com.ascend.flockr.model.task.rule;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.reactivex.Completable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRule extends Rule {

  @JsonIgnore private Map<String, Object> properties;

  private Map<String, Map<String, List<String>>> attributeMapping;

  @JsonAnySetter
  public void setProperty(String propertyKey, Object value) {
    if (this.properties == null) {
      this.properties = new HashMap<>();
    }

    this.properties.put(propertyKey, value);
  }

  @JsonAnyGetter
  public Map<String, Object> getProperty() {
    return properties;
  }

  @Override
  public Completable validate() {
    return Completable.complete();
  }
}
