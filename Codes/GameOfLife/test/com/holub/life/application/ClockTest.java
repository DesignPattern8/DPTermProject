package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.DisplayName;
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
      Clock clock = new Clock(new UtilTimerTickSource(timer));
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

  /** Manual TickSource: records the requested interval and fires ticks on demand. */
  private static class ManualTickSource implements TickSource {
    int interval = -1;
    Runnable onTick;
    boolean running;

    @Override
    public void start(int intervalMillis, Runnable onTick) {
      this.interval = intervalMillis;
      this.onTick = onTick;
      running = true;
    }

    @Override
    public void stop() {
      running = false;
    }

    void fire() {
      onTick.run();
    }
  }

  @Test
  @DisplayName("APP-R01 속도별 시작은 Speed의 주기로 TickSource를 시작한다")
  void startUsesSpeedInterval() {
    ManualTickSource source = new ManualTickSource();
    Clock clock = new Clock(source);
    Speed[] speeds = {Speed.AGONIZING, Speed.SLOW, Speed.MEDIUM, Speed.FAST};
    int[] intervals = {500, 150, 70, 30};

    for (int i = 0; i < speeds.length; i++) {
      clock.start(speeds[i]);
      assertEquals(intervals[i], source.interval);
      assertTrue(source.running);
    }
  }

  @Test
  @DisplayName("APP-R02 TickSource의 tick은 모든 Clock 리스너에게 전달된다")
  void tickSourceDrivesEveryListener() {
    ManualTickSource source = new ManualTickSource();
    Clock clock = new Clock(source);
    int[] first = {0};
    int[] second = {0};
    clock.addClockListener(() -> first[0]++);
    clock.addClockListener(() -> second[0]++);
    clock.start(Speed.FAST);

    source.fire();
    source.fire();

    assertEquals(2, first[0]);
    assertEquals(2, second[0]);
  }

  @Test
  @DisplayName("APP-R03 주기 0은 정지이고, 정지 상태에서 다시 정지해도 된다")
  void zeroIntervalAndRepeatedStopLeaveSourceStopped() {
    ManualTickSource source = new ManualTickSource();
    Clock clock = new Clock(source);
    clock.start(Speed.SLOW);

    clock.startTicking(0);
    assertFalse(source.running);
    clock.stop();
    clock.stop();

    assertFalse(source.running);
  }
}
