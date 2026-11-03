package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class ClockTest {
  private static class RecordingTimer extends Timer {
    TimerTask task;
    long delay, period;

    RecordingTimer() {
      super(true);
    }

    @Override
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
      this.task = task;
      this.delay = delay;
      this.period = period;
      super.scheduleAtFixedRate(task, 86_400_000, period);
    }
  }

  @Test
  void scheduleAndStopKeepOriginalTiming() {
    RecordingTimer timer = new RecordingTimer();
    try {
      Clock clock = new Clock(timer);
      int[] ticks = {0};
      clock.addClockListener(() -> ticks[0]++);
      clock.startTicking(70);
      assertEquals(0, timer.delay);
      assertEquals(70, timer.period);
      timer.task.run();
      assertEquals(1, ticks[0]);
      TimerTask first = timer.task;
      clock.startTicking(30);
      assertFalse(first.cancel());
      assertEquals(30, timer.period);
      clock.stop();
      assertFalse(timer.task.cancel());
      clock.tick();
      assertEquals(2, ticks[0]);
    } finally {
      timer.cancel();
    }
  }
}
