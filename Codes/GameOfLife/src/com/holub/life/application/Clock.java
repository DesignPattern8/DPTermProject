package com.holub.life.application;

import com.holub.tools.Publisher;
import java.util.*;

/** Existing timer and tick publisher, separated from Swing menu code. */
public class Clock {
  private final Timer clock;
  private TimerTask tick = null;

  private Clock() {
    this(new Timer());
  }

  public Clock(Timer timer) {
    clock = timer;
  }

  private static Clock instance;

  /**
   * Return the existing application-wide clock. Tests may supply their own Timer to the
   * constructor.
   */
  public static synchronized Clock instance() {
    if (instance == null) instance = new Clock();
    return instance;
  }

  /**
   * Start up the clock.
   *
   * @param millisecondsBetweenTicks The number of milliseconds between ticks. A value of 0
   *     indicates that the clock should be stopped.
   */
  public void startTicking(int millisecondsBetweenTicks) {
    if (tick != null) {
      tick.cancel();
      tick = null;
    }

    if (millisecondsBetweenTicks > 0) {
      tick =
          new TimerTask() {
            public void run() {
              tick();
            }
          };
      clock.scheduleAtFixedRate(tick, 0, millisecondsBetweenTicks);
    }
  }

  /** Stop the clock */
  public void stop() {
    startTicking(0);
  }

  private Publisher publisher = new Publisher();

  /**
   * Add a listener that's notified every time the clock ticks:
   *
   * <PRE>
   *  Clock.instance().addClockListener
   *  (	new Clock.Listener()
   *  	{	public void tick()
   *  		{	System.out.println("tick!");
   *  		}
   * 	}
   *  );
   *  </PRE>
   */
  public void addClockListener(Listener observer) {
    publisher.subscribe(observer);
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
    publisher.publish(
        new Publisher.Distributor() {
          public void deliverTo(Object subscriber) {
            ((Listener) subscriber).tick();
          }
        });
  }
}
