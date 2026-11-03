package com.holub.life.ui;

import com.holub.life.application.TickSource;
import javax.swing.Timer;

/**
 * A TickSource that fires on the Swing event thread. Because ticks, mouse clicks and painting all
 * run on that one thread, the board is never drawn while it is being updated. The controller still
 * decides when to start, stop, and what a tick does.
 */
public final class SwingTickSource implements TickSource {
  private Timer timer = null;

  @Override
  public void start(int intervalMillis, Runnable onTick) {
    stop();
    if (intervalMillis <= 0) return;

    timer = new Timer(intervalMillis, e -> onTick.run());
    timer.setInitialDelay(0); // the original Clock also fired immediately
    timer.start();
  }

  @Override
  public void stop() {
    if (timer != null) {
      timer.stop();
      timer = null;
    }
  }
}
