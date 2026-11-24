package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

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
      calls.add("snapshot");
      return STORED;
    }

    @Override
    public void restore(BoardSnapshot snapshot) {
      calls.add(snapshot.equals(STORED) ? "restore:stored" : "restore:other");
    }
  }

  private static final BoardSnapshot STORED =
      new BoardSnapshot(64, 64, Set.of(new CellPosition(3, 4), new CellPosition(40, 50)));

  private Timer timer;
  private RecordingBoard board;
  private GameController controller;
  private List<GameListener.Change> changes;

  @BeforeEach
  void setUp() {
    timer = new Timer(true);
    board = new RecordingBoard();
    controller = new GameController(board, new Clock(new UtilTimerTickSource(timer)));
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

  @Test
  @DisplayName("PRE-10 프리셋은 스냅샷을 한 번 보고 죽은 칸만 toggle한다")
  void presetReadsSnapshotOnceAndTogglesOnlyDeadCells() {
    // Given: STORED has (3,4) alive

    // When
    PresetResult result = controller.applyPreset("block", new CellPosition(3, 3));

    // Then
    assertEquals(PresetResult.APPLIED, result);
    assertEquals(List.of("snapshot", "toggle:3,3", "toggle:4,3", "toggle:4,4"), board.calls);
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  @DisplayName("PRE-11 다른 스레드의 tick이 진행 중이면 프리셋 배치 전체가 끝날 때까지 기다린다")
  void presetWaitsForRunningStep() throws Exception {
    board.stepEntered = new CountDownLatch(1);
    board.releaseStep = new CountDownLatch(1);
    ExecutorService threads = Executors.newFixedThreadPool(2);
    try {
      Future<Boolean> tick = threads.submit(controller::tick);
      assertTrue(board.stepEntered.await(5, TimeUnit.SECONDS));

      Future<PresetResult> place =
          threads.submit(() -> controller.applyPreset("blinker", new CellPosition(10, 10)));
      // Give the placement time to reach the board if nothing stops it.
      assertThrows(TimeoutException.class, () -> place.get(200, TimeUnit.MILLISECONDS));
      assertEquals(List.of("step:start"), board.calls);

      board.releaseStep.countDown();
      tick.get(5, TimeUnit.SECONDS);
      assertEquals(PresetResult.APPLIED, place.get(5, TimeUnit.SECONDS));

      assertEquals(
          List.of(
              "step:start",
              "step:end",
              "snapshot",
              "toggle:10,10",
              "toggle:10,11",
              "toggle:10,12"),
          board.calls);
    } finally {
      board.releaseStep.countDown();
      threads.shutdownNow();
    }
  }

  @Test
  @DisplayName("SNAP-08 저장은 Board.snapshot() 1번, 불러오기는 Board.restore() 1번만 부른다")
  void storeAndLoadUseBoardSnapshotOnly(@TempDir Path directory) throws Exception {
    // Given
    Path file = directory.resolve("board.life");

    // When
    controller.store(file.toFile());
    controller.load(file.toFile());

    // Then: no clear/toggle around restore, no file-only cell path
    assertEquals(List.of("snapshot", "restore:stored"), board.calls);
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }
}
