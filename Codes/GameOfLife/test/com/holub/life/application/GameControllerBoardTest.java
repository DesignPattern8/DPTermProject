package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

/** GameController against a Board test double, without the classic cell tree. */
class GameControllerBoardTest {
  /** Records calls; step() returns {@code changed} and can be held open with a latch. */
  private static class RecordingBoard implements Board {
    final List<String> calls = Collections.synchronizedList(new ArrayList<>());
    volatile boolean changed;
    volatile CountDownLatch stepEntered = new CountDownLatch(0);
    volatile CountDownLatch releaseStep = new CountDownLatch(0);

    @Override
    public int rows() {
      return 64;
    }

    @Override
    public int columns() {
      return 64;
    }

    @Override
    public boolean step() {
      calls.add("step:start");
      stepEntered.countDown();
      try {
        assertTrue(releaseStep.await(5, TimeUnit.SECONDS), "step was never released");
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      calls.add("step:end");
      return changed;
    }

    @Override
    public void toggle(CellPosition position) {
      calls.add("toggle:" + position.row() + "," + position.column());
    }

    @Override
    public void clear() {
      calls.add("clear");
    }

    @Override
    public BoardSnapshot snapshot() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void restore(BoardSnapshot snapshot) {
      throw new UnsupportedOperationException();
    }
  }

  private Timer timer;
  private RecordingBoard board;
  private GameController controller;
  private List<GameListener.Change> changes;

  @BeforeEach
  void setUp() {
    timer = new Timer(true);
    board = new RecordingBoard();
    controller =
        new GameController(board, Cell.DUMMY, new Clock(new UtilTimerTickSource(timer)));
    changes = Collections.synchronizedList(new ArrayList<>());
    controller.addGameListener(changes::add);
  }

  @AfterEach
  void cleanUp() {
    timer.cancel();
  }

  @Test
  @DisplayName("APP-B01 tick은 Board.step() 한 번으로 진행하고, 바뀌었으면 부분 갱신을 알린다")
  void tickStepsBoardOnceAndReportsChange() {
    board.changed = true;

    assertTrue(controller.tick());

    assertEquals(List.of("step:start", "step:end"), board.calls);
    assertEquals(List.of(GameListener.Change.GENERATION), changes);
  }

  @Test
  @DisplayName("APP-B02 바뀐 칸이 없는 세대는 알리지 않는다")
  void unchangedStepIsNotReported() {
    board.changed = false;

    assertFalse(controller.tick());

    assertEquals(List.of("step:start", "step:end"), board.calls);
    assertTrue(changes.isEmpty());
  }

  @Test
  @DisplayName("APP-B03 셀 편집은 행·열 그대로 Board.toggle에 전달된다")
  void toggleForwardsRowAndColumn() {
    controller.toggle(3, 7);

    assertEquals(List.of("toggle:3,7"), board.calls);
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  @DisplayName("APP-B04 Clear는 Board.clear에 전달된다")
  void clearForwardsToBoard() {
    controller.clear();

    assertEquals(List.of("clear"), board.calls);
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  @DisplayName("APP-B05 다른 스레드의 tick이 진행 중이면 편집은 끝날 때까지 보드에 들어가지 않는다")
  void editWaitsForRunningStep() throws Exception {
    board.stepEntered = new CountDownLatch(1);
    board.releaseStep = new CountDownLatch(1);
    ExecutorService threads = Executors.newFixedThreadPool(2);
    try {
      Future<Boolean> tick = threads.submit(controller::tick);
      assertTrue(board.stepEntered.await(5, TimeUnit.SECONDS));

      Future<?> edit = threads.submit(() -> controller.toggle(1, 2));
      // Give the edit time to reach the board if nothing stops it.
      assertThrows(TimeoutException.class, () -> edit.get(200, TimeUnit.MILLISECONDS));
      assertEquals(List.of("step:start"), board.calls);

      board.releaseStep.countDown();
      tick.get(5, TimeUnit.SECONDS);
      edit.get(5, TimeUnit.SECONDS);

      assertEquals(List.of("step:start", "step:end", "toggle:1,2"), board.calls);
    } finally {
      board.releaseStep.countDown();
      threads.shutdownNow();
    }
  }
}
