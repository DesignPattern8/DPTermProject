package com.holub.life.twoplayer.ui;

import com.holub.asynch.ConditionVariable;
import com.holub.life.application.Clock;
import com.holub.life.application.Speed;
import com.holub.life.application.UtilTimerTickSource;
import com.holub.life.model.Cell;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;
import com.holub.life.twoplayer.NeighborhoodTwoPlayerBoard;
import com.holub.life.twoplayer.Player;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.Timer;

/**
 * #74 확인 (docs/007-two-player-ui.md "#74 확인"). 클래식 보드와 2인용 보드가 한 프로세스에 있을 때 {@code Neighborhood}의
 * {@code static} 잠금이 어떻게 되는지 잰다. {@code @Test}가 없어 {@code ./gradlew test}에서는 돌지 않는다.
 *
 * <pre>
 * ./gradlew testClasses
 * java -cp build/classes/java/main:build/classes/java/test com.holub.life.twoplayer.ui.StaticLockCheck
 * </pre>
 */
public final class StaticLockCheck {
  private static Field nesting;
  private static Field permitted;

  private StaticLockCheck() {}

  public static void main(String[] arguments) throws Exception {
    nesting = Neighborhood.class.getDeclaredField("nestingLevel");
    nesting.setAccessible(true);
    permitted = Neighborhood.class.getDeclaredField("readingPermitted");
    permitted.setAccessible(true);
    System.out.println(
        "java "
            + System.getProperty("java.version")
            + ", cpus "
            + Runtime.getRuntime().availableProcessors());

    // S1: 한 스레드에서 한 번에 한 보드만 진행
    Neighborhood classic = classic();
    NeighborhoodTwoPlayerBoard twoPlayer = twoPlayer();
    for (int i = 0; i < 500; i++) step(classic);
    for (int i = 0; i < 500; i++) twoPlayer.advanceGeneration();
    report("S1 sequential", classic, 200_000);

    // S2: 두 스레드에서 두 보드를 동시에 1초 동안 진행
    for (int trial = 1; trial <= 5; trial++) {
      Neighborhood c = classic();
      NeighborhoodTwoPlayerBoard t = twoPlayer();
      long end = System.nanoTime() + 1_000_000_000L;
      Thread a = new Thread(() -> {
        while (System.nanoTime() < end) step(c);
      });
      Thread b = new Thread(() -> {
        while (System.nanoTime() < end) t.advanceGeneration();
      });
      a.start();
      b.start();
      a.join();
      b.join();
      report("S2 concurrent trial " + trial, c, 10_000);
      reset();
    }

    // S3: 모드마다 시계 하나씩, 둘 다 Fast(30 ms)
    Neighborhood c = classic();
    NeighborhoodTwoPlayerBoard t = twoPlayer();
    Clock classicClock = new Clock(new UtilTimerTickSource(new Timer(true)));
    Clock twoPlayerClock = new Clock(new UtilTimerTickSource(new Timer(true)));
    classicClock.addClockListener(() -> step(c));
    twoPlayerClock.addClockListener(t::advanceGeneration);
    classicClock.start(Speed.FAST);
    twoPlayerClock.start(Speed.FAST);
    int seconds = 0;
    while (seconds < 30 && nesting.getInt(null) == -1) {
      Thread.sleep(1000);
      seconds++;
    }
    classicClock.stop();
    twoPlayerClock.stop();
    Thread.sleep(200);
    report("S3 two clocks at Fast, stopped after " + seconds + " s", c, 10_000);
  }

  private static Neighborhood classic() {
    Neighborhood board = new Neighborhood(8, new Neighborhood(8, new Resident()));
    Random random = new Random(7);
    for (int i = 0; i < 1500; i++) board.toggle(random.nextInt(64), random.nextInt(64));
    return board;
  }

  private static NeighborhoodTwoPlayerBoard twoPlayer() {
    NeighborhoodTwoPlayerBoard board = new NeighborhoodTwoPlayerBoard();
    Random random = new Random(11);
    for (int i = 0; i < 1500; i++) {
      int row = random.nextInt(64);
      int column = random.nextInt(64);
      if (board.isEmpty(row, column)) board.place(row, column, column < 32 ? Player.A : Player.B);
    }
    return board;
  }

  private static void step(Neighborhood board) {
    Cell d = Cell.DUMMY;
    if (board.figureNextState(d, d, d, d, d, d, d, d)) board.transition();
  }

  private static void report(String label, Neighborhood classic, int redraws) throws Exception {
    int skipped = 0;
    for (int i = 0; i < redraws; i++) if (!classic.beginRedraw(true)) skipped++;
    System.out.println(
        label
            + ": nestingLevel="
            + nesting.getInt(null)
            + " readingPermitted="
            + ((ConditionVariable) permitted.get(null)).isTrue()
            + " classic beginRedraw(true) false="
            + skipped
            + "/"
            + redraws);
  }

  private static void reset() throws Exception {
    nesting.setInt(null, -1);
    ((ConditionVariable) permitted.get(null)).set(true);
  }
}
