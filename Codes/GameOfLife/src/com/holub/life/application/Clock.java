package com.holub.life.application;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Game timing. Decides when the game runs and at what speed, and notifies listeners on every tick.
 * Where the ticks come from is a {@link TickSource}.
 */
public class Clock {
  private final TickSource ticks;
  private final List<Listener> listeners = new CopyOnWriteArrayList<>();

  public Clock(TickSource ticks) {
    this.ticks = ticks;
  }

  /** Run at one of the Go menu speeds. If the clock is already running, only the speed changes. */
  public void start(Speed speed) {
    startTicking(speed.millis());
  }

  /**
   * Start up the clock.
   *
   * @param millisecondsBetweenTicks The number of milliseconds between ticks. A value of 0
   *     indicates that the clock should be stopped.
   */
  public void startTicking(int millisecondsBetweenTicks) {
    if (millisecondsBetweenTicks > 0) ticks.start(millisecondsBetweenTicks, this::tick);
    else ticks.stop();
  }

  /** Stop the clock */
  public void stop() {
    ticks.stop();
  }

  /**
   * Add a listener that's notified every time the clock ticks. As with the original Publisher, the
   * most recently added listener is notified first.
   */
  public void addClockListener(Listener observer) {
    listeners.add(0, observer);
  }

  /**
   * Implement this interface to be notified about clock ticks.
   *
   * @see Clock
   */
  public interface Listener {
    void tick();
  }

  /**
   * Force the clock to "tick," even if it's not time for a tick. Useful for forcing a tick when the
   * clock is stopped. (Life uses this for single stepping.)
   */
  public void tick() {
    for (Listener listener : listeners) listener.tick();
  }
}
