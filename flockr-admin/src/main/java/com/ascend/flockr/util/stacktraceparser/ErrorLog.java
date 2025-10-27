package com.ascend.flockr.util.stacktraceparser;

import java.util.List;
import lombok.Data;

@Data
public class ErrorLog {
  private String message;
  private List<String> cause;
}
