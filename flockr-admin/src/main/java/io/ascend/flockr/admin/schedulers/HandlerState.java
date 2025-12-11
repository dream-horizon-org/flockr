package io.ascend.flockr.admin.schedulers;

public enum HandlerState {
  WAITING_TRIGGER,
  ACQUIRING_LOCK,
  RUNNING
}
