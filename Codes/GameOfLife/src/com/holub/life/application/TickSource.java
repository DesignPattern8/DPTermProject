package com.holub.life.application;

/**
 * Where clock ticks come from. The clock decides when to start and stop and what a tick does; a
 * TickSource only calls {@code onTick} every {@code intervalMillis} milliseconds.
 */
public interface TickSource {
  /**
   * Start calling {@code onTick} every {@code intervalMillis} milliseconds, beginning immediately.
   * Any previously started ticking is cancelled first. Callers pass a positive interval.
   */
  void start(int intervalMillis, Runnable onTick);

  /** Stop ticking. Calling this when already stopped does nothing. */
  void stop();
}
