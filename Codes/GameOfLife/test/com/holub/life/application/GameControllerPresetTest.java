package com.holub.life.application;

import static com.holub.life.application.CancerGameTest.NO_NEW_CANCER;
import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Preset list and placement (#62) on the real 64x64 classic board. */
class GameControllerPresetTest {
  /** Manual TickSource that only records whether the clock is running. */
  private static final class ManualTickSource implements TickSource {
    boolean running;

    @Override
    public void start(int intervalMillis, Runnable onTick) {
      running = true;
    }

    @Override
    public void stop() {
      running = false;
    }
  }

  private Neighborhood cells;
  private CompositeBoard board;
  private ManualTickSource ticks;
  private Clock clock;
  private GameController controller;
  private final List<GameListener.Change> changes = new ArrayList<>();

  @BeforeEach
  void setUp() {
    cells = new Neighborhood(8, new Neighborhood(8, new Resident()));
    board = new CompositeBoard(cells);
    ticks = new ManualTickSource();
    clock = new Clock(ticks);
    controller = new GameController(board, clock);
    controller.addGameListener(changes::add);
  }

  private static CellPosition at(int row, int column) {
    return new CellPosition(row, column);
  }

  @Test
  @DisplayName("PRE-01 프리셋 목록은 카탈로그 순서의 id와 표시 이름이다")
  void presetsFollowCatalogOrder() {
    // When
    List<PresetChoice> presets = controller.presets();

    // Then
    assertEquals(
        List.of(
            new PresetChoice("block", "Block"),
            new PresetChoice("blinker", "Blinker"),
            new PresetChoice("glider", "Glider")),
        presets);
    assertEquals(Set.of(), board.snapshot().liveCells());
    assertTrue(changes.isEmpty());
  }

  @Test
  @DisplayName("PRE-02 프리셋 좌상단을 원점에 맞춰 배치하고 EDITED를 한 번 알린다")
  void gliderIsPlacedRelativeToOrigin() {
    // When
    PresetResult result = controller.applyPreset("glider", at(10, 20));

    // Then
    assertEquals(PresetResult.APPLIED, result);
    assertEquals(
        Set.of(at(10, 21), at(11, 22), at(12, 20), at(12, 21), at(12, 22)),
        board.snapshot().liveCells());
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  @DisplayName("PRE-03 보드 오른쪽 아래 끝에 꼭 맞는 프리셋은 배치된다")
  void presetFittingBottomRightCornerIsApplied() {
    // When
    PresetResult result = controller.applyPreset("block", at(62, 62));

    // Then
    assertEquals(PresetResult.APPLIED, result);
    assertEquals(
        Set.of(at(62, 62), at(62, 63), at(63, 62), at(63, 63)), board.snapshot().liveCells());
  }

  @Test
  @DisplayName("PRE-04 한 칸이라도 보드 밖이면 아무것도 놓지 않고 알리지 않는다")
  void presetCrossingAnyEdgeChangesNothing() {
    // Given
    board.restore(new BoardSnapshot(64, 64, Set.of(at(1, 1))));
    BoardSnapshot before = board.snapshot();

    // When
    PresetResult bottom = controller.applyPreset("block", at(63, 62));
    PresetResult top = controller.applyPreset("blinker", at(-1, 0));
    PresetResult right = controller.applyPreset("blinker", at(0, 62));

    // Then
    assertEquals(PresetResult.OUT_OF_BOUNDS, bottom);
    assertEquals(PresetResult.OUT_OF_BOUNDS, top);
    assertEquals(PresetResult.OUT_OF_BOUNDS, right);
    assertEquals(before, board.snapshot());
    assertTrue(changes.isEmpty());
  }

  @Test
  @DisplayName("PRE-05 이미 산 칸과 겹치면 그 칸은 산 채로 둔다")
  void overlappingLiveCellStaysAlive() {
    // Given
    board.restore(new BoardSnapshot(64, 64, Set.of(at(5, 5))));

    // When
    PresetResult result = controller.applyPreset("block", at(5, 5));

    // Then
    assertEquals(PresetResult.APPLIED, result);
    assertEquals(Set.of(at(5, 5), at(5, 6), at(6, 5), at(6, 6)), board.snapshot().liveCells());
  }

  @Test
  @DisplayName("PRE-06 암세포 칸과 겹쳐도 암세포는 그대로 둔다")
  void overlappingCancerCellIsLeftUntouched() {
    // Given
    GameController cancerController =
        new GameController(board, clock, new CancerGame(cells, NO_NEW_CANCER));
    board.restore(new BoardSnapshot(64, 64, Set.of(at(5, 5)), Set.of(at(5, 5))));

    // When
    PresetResult result = cancerController.applyPreset("block", at(5, 5));

    // Then
    assertEquals(PresetResult.APPLIED, result);
    BoardSnapshot after = board.snapshot();
    assertEquals(Set.of(at(5, 5), at(5, 6), at(6, 5), at(6, 6)), after.liveCells());
    assertEquals(Set.of(at(5, 5)), after.cancerCells());
  }

  @Test
  @DisplayName("PRE-07 없는 id는 보드를 바꾸지 않고 알리지 않는다")
  void unknownIdChangesNothing() {
    // Given
    board.restore(new BoardSnapshot(64, 64, Set.of(at(1, 1))));
    BoardSnapshot before = board.snapshot();

    // When
    PresetResult result = controller.applyPreset("nope", at(0, 0));

    // Then
    assertEquals(PresetResult.UNKNOWN_ID, result);
    assertEquals(before, board.snapshot());
    assertTrue(changes.isEmpty());
  }

  @Test
  @DisplayName("PRE-08 실행 중에 배치해도 시계는 멈추지 않는다")
  void placingDoesNotStopRunningClock() {
    // Given
    clock.start(Speed.FAST);
    assertTrue(ticks.running);

    // When
    PresetResult result = controller.applyPreset("block", at(0, 0));

    // Then
    assertEquals(PresetResult.APPLIED, result);
    assertTrue(ticks.running);
  }

  @Test
  @DisplayName("PRE-09 원점이 없으면 NullPointerException이고 보드는 그대로다")
  void nullOriginIsRejected() {
    // When / Then
    assertThrows(NullPointerException.class, () -> controller.applyPreset("block", null));
    assertEquals(Set.of(), board.snapshot().liveCells());
    assertTrue(changes.isEmpty());
  }
}
