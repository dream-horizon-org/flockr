package io.ascend.flockr.admin.service.schedulers;

public enum HandlerState {
  WAITING_TRIGGER,
  ACQUIRING_LOCK,
  RUNNING,
}
