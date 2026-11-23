package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import com.holub.life.persistence.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** #88: Store and Load go through Board.snapshot()/restore() and C's snapshot file API. */
class GameControllerSnapshotFileTest {
  @TempDir Path directory;
  private final Neighborhood root = new Neighborhood(8, new Neighborhood(8, new Resident()));
  private final CompositeBoard board = new CompositeBoard(root);
  private final ManualTicks ticks = new ManualTicks();
  private final Clock clock = new Clock(ticks);
  private final GameController controller =
      new GameController(
          board,
          clock,
          null,
          new BoardFileStore(new LegacyLifeCodec(), List.of(8, 1)));
  private final List<GameListener.Change> changes = new ArrayList<>();

  GameControllerSnapshotFileTest() {
    controller.addGameListener(changes::add);
  }

  private static final class ManualTicks implements TickSource {
    int starts;
    int stops;

    @Override
    public void start(int intervalMillis, Runnable tick) {
      starts++;
    }

    @Override
    public void stop() {
      stops++;
    }
  }

  @Test
  @DisplayName("SNAP-01 블록 경계에 걸친 일반 세포가 Store → Clear → Load 후 같은 스냅샷으로 돌아온다")
  void classicRoundTripRestoresSameSnapshot() throws Exception {
    // Given: three cells across the (8, 8) block corner
    controller.toggle(7, 7);
    controller.toggle(7, 8);
    controller.toggle(8, 7);
    BoardSnapshot before = board.snapshot();
    Path file = directory.resolve("classic.life");

    // When
    controller.store(file.toFile());
    controller.clear();
    changes.clear();
    controller.load(file.toFile());

    // Then
    assertEquals(before, board.snapshot());
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  @DisplayName("SNAP-02 일반·암세포 혼합 보드가 Store → Clear → Load 후 두 종류 모두 그대로 돌아온다")
  void cancerRoundTripRestoresBothCellKinds() throws Exception {
    // Given
    CellPosition cancer = new CellPosition(30, 40);
    board.restore(
        new BoardSnapshot(
            64,
            64,
            Set.of(new CellPosition(1, 1), new CellPosition(10, 20), cancer),
            Set.of(cancer)));
    BoardSnapshot before = board.snapshot();
    Path file = directory.resolve("cancer.life");

    // When
    controller.store(file.toFile());
    controller.clear();
    controller.load(file.toFile());

    // Then
    assertEquals(before.liveCells(), board.snapshot().liveCells());
    assertEquals(Set.of(cancer), board.snapshot().cancerCells());
  }

  @Test
  @DisplayName("SNAP-06 범위 밖 암세포 좌표는 범위 밖 일반 세포 좌표처럼 무시한다")
  void outOfRangeCancerIsIgnoredLikeLiveCells() throws Exception {
    // Given
    CellPosition inside = new CellPosition(1, 1);
    CellPosition outside = new CellPosition(64, 0);
    Path file = patternFile(new PatternData(List.of(inside, outside), Set.of(outside)));

    // When
    controller.load(file.toFile());

    // Then
    assertEquals(new BoardSnapshot(64, 64, Set.of(inside)), board.snapshot());
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  @DisplayName("SNAP-09 생존 목록에 없는 암세포 좌표가 든 파일은 거부하고 보드·Clock을 그대로 둔다")
  void cancerOutsideLiveCellsIsRejected() throws Exception {
    // Given: a running clock and a mixed board
    CellPosition kept = new CellPosition(5, 6);
    board.restore(new BoardSnapshot(64, 64, Set.of(kept, new CellPosition(9, 9)), Set.of(kept)));
    BoardSnapshot before = board.snapshot();
    clock.start(Speed.MEDIUM);
    Path file =
        patternFile(new PatternData(List.of(new CellPosition(1, 1)), Set.of(new CellPosition(2, 2))));
    int stopsBefore = ticks.stops;

    // When
    assertThrows(IOException.class, () -> controller.load(file.toFile()));

    // Then
    assertEquals(before, board.snapshot());
    assertEquals(1, ticks.starts);
    assertEquals(stopsBefore, ticks.stops);
    assertTrue(changes.isEmpty());
  }

  private Path patternFile(PatternData data) throws IOException {
    Path file = directory.resolve("pattern.life");
    new BoardFileStore(new LegacyLifeCodec()).write(file, data);
    return file;
  }
}
