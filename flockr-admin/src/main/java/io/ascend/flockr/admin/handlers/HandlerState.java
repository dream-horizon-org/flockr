package io.ascend.flockr.admin.handlers;

/**
 * Represents the execution state of a scheduled handler.
 *
 * <p>This enum is used to track the lifecycle of handler execution and prevent concurrent
 * executions. The state transitions follow this sequence:
 *
 * <pre>
 * WAITING_TRIGGER → ACQUIRING_LOCK → RUNNING → WAITING_TRIGGER
 * </pre>
 *
 * @author Prithu Sharma
 * @since 1.0
 */
public enum HandlerState {

  /** Handler is idle, waiting for the next scheduled trigger */
  WAITING_TRIGGER,

  /** Handler is attempting to acquire a distributed lease */
  ACQUIRING_LOCK,

  /** Handler has acquired the lease and is actively processing */
  RUNNING
}
