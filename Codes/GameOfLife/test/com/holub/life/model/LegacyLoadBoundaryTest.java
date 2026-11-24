package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.application.TickSource;
import com.holub.life.persistence.BoardFileStore;
import com.holub.life.persistence.LegacyLifeCodec;
import com.holub.life.persistence.PatternData;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LegacyLoadBoundaryTest {
  @TempDir Path directory;
  private final BoardFileStore files = new BoardFileStore(new LegacyLifeCodec());

  @ParameterizedTest(name = "gridSize={0}, depth={1}, center=({2},{3}), horizontal={4}")
  @CsvSource({
    "8, 2, 10, 10, true",
    "8, 2, 7, 8, true",
    "8, 2, 8, 7, false",
    "8, 2, 7, 7, true",
    "8, 2, 7, 7, false",
    "8, 2, 0, 8, true",
    "8, 2, 63, 8, true",
    "8, 2, 8, 0, false",
    "8, 2, 8, 63, false",
    "2, 3, 3, 4, true",
    "2, 3, 4, 3, false"
  })
  @DisplayName("LDB-01 파일로 불러온 블링커는 경계에서도 직접 편집과 같은 네 세대를 만든다")
  void loadedBlinkerMatchesDirectEditing(
      int gridSize, int depth, int centerRow, int centerColumn, boolean horizontal)
      throws Exception {
    // Given
    Neighborhood root = neighborhood(gridSize, depth);
    Board loaded = new CompositeBoard(root);
    Board edited = new CompositeBoard(neighborhood(gridSize, depth));
    Set<CellPosition> initial = line(centerRow, centerColumn, horizontal, loaded.rows());
    initial.forEach(edited::toggle);
    Path file = directory.resolve("blinker.life");
    files.write(file, new PatternData(List.copyOf(initial), Set.of()));
    GameController controller = controller(loaded, root);

    // When
    controller.load(file.toFile());

    // Then
    assertEquals(edited.snapshot(), loaded.snapshot());
    Set<CellPosition> firstGeneration =
        line(centerRow, centerColumn, !horizontal, loaded.rows());
    for (int generation = 1; generation <= 4; generation++) {
      assertEquals(edited.step(), controller.tick(), "세대 " + generation + " 변경 여부");
      BoardSnapshot actual = loaded.snapshot();
      if (generation == 1) {
        assertEquals(new BoardSnapshot(loaded.rows(), loaded.columns(), firstGeneration), actual);
      }
      assertEquals(edited.snapshot(), actual, "세대 " + generation);
    }
  }

  @ParameterizedTest(name = "center=({0},{1}), horizontal={2}")
  @CsvSource({"7, 8, true", "8, 7, false", "7, 7, true"})
  @DisplayName("LDB-02 파일로 불러온 암세포는 경계 너머에서도 태어나고 직접 편집과 같이 진행한다")
  void loadedCancerMatchesDirectEditing(int centerRow, int centerColumn, boolean horizontal)
      throws Exception {
    // Given
    Neighborhood root = neighborhood(8, 2);
    Board loaded = new CompositeBoard(root);
    Neighborhood editedRoot = neighborhood(8, 2);
    Board edited = new CompositeBoard(editedRoot);
    Set<CellPosition> initial = line(centerRow, centerColumn, horizontal, 64);
    for (CellPosition position : initial) {
      edited.toggle(position);
      CancerModelTest.cellAt(editedRoot, position.row(), position.column()).setCancer();
    }
    Path file = directory.resolve("cancer.life");
    files.write(file, new PatternData(List.copyOf(initial), initial));
    GameController controller = controller(loaded, root);

    // When
    controller.load(file.toFile());

    // Then
    assertEquals(edited.snapshot(), loaded.snapshot());
    Set<CellPosition> firstGeneration =
        Set.of(
            new CellPosition(centerRow, centerColumn),
            new CellPosition(centerRow - 1, centerColumn),
            new CellPosition(centerRow + 1, centerColumn),
            new CellPosition(centerRow, centerColumn - 1),
            new CellPosition(centerRow, centerColumn + 1));
    for (int generation = 1; generation <= 4; generation++) {
      assertEquals(edited.step(), controller.tick(), "세대 " + generation + " 변경 여부");
      BoardSnapshot actual = loaded.snapshot();
      if (generation == 1) {
        assertEquals(new BoardSnapshot(64, 64, firstGeneration, firstGeneration), actual);
      }
      assertEquals(edited.snapshot(), actual, "세대 " + generation);
    }
  }

  @Test
  @DisplayName("LDB-03 빈 파일을 불러오면 경계의 일반·암세포가 사라지고 다음 세대에도 비어 있다")
  void emptyFileClearsBoundaryCells() throws Exception {
    // Given
    Neighborhood root = neighborhood(8, 2);
    Board board = new CompositeBoard(root);
    board.toggle(new CellPosition(7, 7));
    board.toggle(new CellPosition(7, 8));
    board.toggle(new CellPosition(8, 7));
    CancerModelTest.cellAt(root, 7, 8).setCancer();
    Path file = directory.resolve("empty.life");
    files.write(file, new PatternData(List.of(), Set.of()));
    GameController controller = controller(board, root);

    // When
    controller.load(file.toFile());

    // Then
    BoardSnapshot empty = new BoardSnapshot(64, 64, Set.of());
    assertEquals(empty, board.snapshot());
    assertFalse(controller.tick());
    assertEquals(empty, board.snapshot());
  }

  @Test
  @DisplayName("LDB-04 Clear 없이 Memento를 불러와도 기존 일반·암세포를 교체하고 경계에서 태어난다")
  void directMementoLoadReplacesCellsAndActivatesEdges() {
    // Given
    Neighborhood root = neighborhood(8, 2);
    Board loaded = new CompositeBoard(root);
    loaded.toggle(new CellPosition(20, 20));
    loaded.toggle(new CellPosition(40, 40));
    CancerModelTest.cellAt(root, 40, 40).setCancer();
    Board edited = new CompositeBoard(neighborhood(8, 2));
    Set<CellPosition> initial = line(7, 8, true, 64);
    NeighborhoodState state = new NeighborhoodState();
    for (CellPosition position : initial) {
      state.markAsAlive(position);
      edited.toggle(position);
    }

    // When
    assertTrue(root.transfer(state, new CellPosition(0, 0), Cell.LOAD));

    // Then
    assertEquals(new BoardSnapshot(64, 64, initial), loaded.snapshot());
    assertEquals(edited.step(), loaded.step());
    Set<CellPosition> expected =
        Set.of(new CellPosition(6, 8), new CellPosition(7, 8), new CellPosition(8, 8));
    assertEquals(new BoardSnapshot(64, 64, expected), loaded.snapshot());
    assertEquals(edited.snapshot(), loaded.snapshot());
  }

  @Test
  @DisplayName("LDB-05 Clear한 3중 보드에 빈 Memento를 불러와도 모든 블록이 비활성으로 남는다")
  void emptyMementoLeavesClearedBlocksInactive() {
    // Given
    Neighborhood root = neighborhood(2, 3);
    Board board = new CompositeBoard(root);
    board.toggle(new CellPosition(3, 4));
    board.clear();

    // When
    boolean changed = root.transfer(new NeighborhoodState(), new CellPosition(0, 0), Cell.LOAD);

    // Then
    assertFalse(changed);
    assertInactive(root);
    BoardSnapshot empty = new BoardSnapshot(8, 8, Set.of());
    assertEquals(empty, board.snapshot());
    assertFalse(board.step());
    assertEquals(empty, board.snapshot());
  }

  private static Neighborhood neighborhood(int gridSize, int depth) {
    Cell cell = new Resident();
    for (int level = 0; level < depth; level++) cell = new Neighborhood(gridSize, cell);
    return (Neighborhood) cell;
  }

  private static GameController controller(Board board, Neighborhood root) {
    TickSource ticks =
        new TickSource() {
          @Override
          public void start(int intervalMillis, Runnable onTick) {}

          @Override
          public void stop() {}
        };
    return new GameController(board, new Clock(ticks));
  }

  private static Set<CellPosition> line(int row, int column, boolean horizontal, int size) {
    Set<CellPosition> cells = new HashSet<>();
    for (int offset = -1; offset <= 1; offset++) {
      int cellRow = row + (horizontal ? 0 : offset);
      int cellColumn = column + (horizontal ? offset : 0);
      if (cellRow >= 0 && cellRow < size && cellColumn >= 0 && cellColumn < size) {
        cells.add(new CellPosition(cellRow, cellColumn));
      }
    }
    return cells;
  }

  private static void assertInactive(Neighborhood block) {
    assertFalse(block.isActive());
    assertTrue(block.isDisruptiveTo().equals(Direction.NONE));
    for (int row = 0; row < block.gridSize(); row++) {
      for (int column = 0; column < block.gridSize(); column++) {
        if (block.childAt(row, column) instanceof Neighborhood child) assertInactive(child);
      }
    }
  }
}
