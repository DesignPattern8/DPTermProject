package com.holub.life.application;

import java.util.Timer;
import java.util.TimerTask;

/**
 * The original clock timing: a {@link java.util.Timer} running {@code scheduleAtFixedRate(task, 0,
 * interval)}, so ticks run on the Timer's thread. The Timer belongs to the caller, which cancels it.
 */
public final class UtilTimerTickSource implements TickSource {
  private final Timer timer;
  private TimerTask task = null;

  public UtilTimerTickSource(Timer timer) {
    this.timer = timer;
  }

  @Override
  public void start(int intervalMillis, Runnable onTick) {
    stop();
    if (intervalMillis <= 0) return;

    task =
        new TimerTask() {
          public void run() {
            onTick.run();
          }
        };
    timer.scheduleAtFixedRate(task, 0, intervalMillis);
  }

  @Override
  public void stop() {
    if (task != null) {
      task.cancel();
      task = null;
    }
  }
}
