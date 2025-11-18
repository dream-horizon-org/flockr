package com.ascend.flockr.domain.rule;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceInfoBasic implements SourceInfo {
  @NotEmpty private Long id;
}
