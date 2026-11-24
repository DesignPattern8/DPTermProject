package com.holub.life.twoplayer.ui;

import com.holub.life.application.Clock;
import com.holub.life.application.Speed;
import com.holub.life.application.UtilTimerTickSource;
import com.holub.life.model.Cell;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;
import com.holub.life.twoplayer.NeighborhoodTwoPlayerBoard;
import com.holub.life.twoplayer.Player;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * #74 보조 스트레스 확인 (docs/007-two-player-ui.md "#74 확인"). 클래식 보드와 2인용 보드를 한 프로세스에서
 * 진행한 뒤 공개 {@code beginRedraw(true)}의 건너뜀 횟수를 잰다. 결정적인 본 검증은 {@code
 * NeighborhoodReadGateTest}이며, 이 main의 결과만으로 동시성 안전성을 증명하지 않는다. {@code @Test}가 없어
 * {@code ./gradlew test}에서는 돌지 않는다.
 *
 * <pre>
 * ./gradlew testClasses
 * java -cp build/classes/java/main:build/classes/java/test com.holub.life.twoplayer.ui.StaticLockCheck
 * </pre>
 */
public final class StaticLockCheck {
  private static final int CLOCK_TICKS = 100;

  private StaticLockCheck() {}

  public static void main(String[] arguments) throws Exception {
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
      Thread a =
          new Thread(
              () -> {
                while (System.nanoTime() < end) step(c);
              });
      Thread b =
          new Thread(
              () -> {
                while (System.nanoTime() < end) t.advanceGeneration();
              });
      a.start();
      b.start();
      a.join();
      b.join();
      report("S2 concurrent trial " + trial, c, 10_000);
    }

    // S3: 모드마다 Fast(30 ms) 시계 하나씩. 경과 시간 대신 각 100tick 완료를 종료 기준으로 삼는다.
    Neighborhood c = classic();
    NeighborhoodTwoPlayerBoard t = twoPlayer();
    Timer classicTimer = new Timer(true);
    Timer twoPlayerTimer = new Timer(true);
    Clock classicClock = new Clock(new UtilTimerTickSource(classicTimer));
    Clock twoPlayerClock = new Clock(new UtilTimerTickSource(twoPlayerTimer));
    CountDownLatch finished = new CountDownLatch(2);
    int[] completedTicks = new int[2];
    // 각 Timer는 자기 슬롯만 변경한다. 마지막 전이가 끝나면 후속 콜백은 모델을 건드리지 않는다.
    classicClock.addClockListener(
        () -> {
          if (completedTicks[0] >= CLOCK_TICKS) return;
          step(c);
          if (++completedTicks[0] == CLOCK_TICKS) finished.countDown();
        });
    twoPlayerClock.addClockListener(
        () -> {
          if (completedTicks[1] >= CLOCK_TICKS) return;
          t.advanceGeneration();
          if (++completedTicks[1] == CLOCK_TICKS) finished.countDown();
        });
    boolean completed;
    try {
      classicClock.start(Speed.FAST);
      twoPlayerClock.start(Speed.FAST);
      completed = finished.await(5, TimeUnit.SECONDS);
    } finally {
      classicClock.stop();
      twoPlayerClock.stop();
      classicTimer.cancel();
      twoPlayerTimer.cancel();
    }
    if (!completed) {
      throw new IllegalStateException("두 시계가 5초 안에 각각 " + CLOCK_TICKS + "tick을 마치지 못했습니다");
    }
    report("S3 two clocks at Fast, completed " + CLOCK_TICKS + " ticks each", c, 10_000);
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
    System.out.println(label + ": classic beginRedraw(true) false=" + skipped + "/" + redraws);
  }
}
