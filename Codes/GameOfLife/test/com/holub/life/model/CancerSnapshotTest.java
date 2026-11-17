package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CancerSnapshotTest {
  private static final int BOARD_SIZE = 64;

  @Test
  @DisplayName("SNAP-C01 캡처·복원 후 일반 세포와 암세포가 같은 위치와 종류로 남는다")
  void restoresLiveAndCancerCellsTogether() {
    // Given
    Neighborhood root = neighborhood();
    Board board = new CompositeBoard(root);
    board.toggle(new CellPosition(10, 10));
    board.toggle(new CellPosition(10, 11));
    CancerModelTest.cellAt(root, 10, 10).setCancer();
    BoardSnapshot captured = board.snapshot();
    board.clear();
    board.toggle(new CellPosition(30, 30));
    CancerModelTest.cellAt(root, 30, 30).setCancer();

    // When
    board.restore(captured);

    // Then
    assertTrue(CancerModelTest.cellAt(root, 10, 10).isAlive());
    assertTrue(CancerModelTest.cellAt(root, 10, 10).isCancer());
    assertTrue(CancerModelTest.cellAt(root, 10, 11).isAlive());
    assertFalse(CancerModelTest.cellAt(root, 10, 11).isCancer());
    assertFalse(CancerModelTest.cellAt(root, 30, 30).isAlive());
    assertFalse(CancerModelTest.cellAt(root, 30, 30).isCancer());
    assertEquals(1, root.cancerCount());
  }

  @Test
  @DisplayName("SNAP-C02 복원 후 첫 세대에 하위 블록 경계 너머에서도 암세포가 태어난다")
  void restoredCancerKeepsBoundaryBirthOnFirstStep() {
    // Given
    Neighborhood originalRoot = neighborhood();
    Board original = new CompositeBoard(originalRoot);
    for (int column = 2; column <= 4; column++) {
      original.toggle(new CellPosition(7, column));
      CancerModelTest.cellAt(originalRoot, 7, column).setCancer();
    }
    Neighborhood restoredRoot = neighborhood();
    Board restored = new CompositeBoard(restoredRoot);
    restored.restore(original.snapshot());

    // When
    original.step();
    restored.step();

    // Then
    assertTrue(CancerModelTest.cellAt(originalRoot, 8, 3).isCancer());
    assertTrue(CancerModelTest.cellAt(restoredRoot, 8, 3).isCancer());
    assertEquals(original.snapshot(), restored.snapshot());
    assertEquals(originalRoot.cancerCount(), restoredRoot.cancerCount());
  }

  @Test
  @DisplayName("SNAP-C03 일반·암세포를 캡처해도 셀 상태와 블록 활성 정보가 바뀌지 않는다")
  void captureKeepsMixedCellStatesAndActiveEdges() {
    // Given
    Neighborhood root = neighborhood();
    Neighborhood inner = (Neighborhood) root.childAt(2, 2);
    Board board = new CompositeBoard(root);
    CellPosition cancer = new CellPosition(23, 20);
    CellPosition normal = new CellPosition(23, 21);
    board.toggle(cancer);
    board.toggle(normal);
    CancerModelTest.cellAt(root, cancer.row(), cancer.column()).setCancer();
    boolean rootActive = root.isActive();
    boolean innerActive = inner.isActive();
    assertTrue(root.isDisruptiveTo().equals(Direction.NONE));
    assertTrue(inner.isDisruptiveTo().equals(Direction.SOUTH));

    // When
    BoardSnapshot captured = board.snapshot();

    // Then
    assertEquals(Set.of(cancer, normal), captured.liveCells());
    assertEquals(Set.of(cancer), captured.cancerCells());
    assertTrue(CancerModelTest.cellAt(root, cancer.row(), cancer.column()).isCancer());
    assertFalse(CancerModelTest.cellAt(root, normal.row(), normal.column()).isCancer());
    assertEquals(rootActive, root.isActive());
    assertEquals(innerActive, inner.isActive());
    assertTrue(root.isDisruptiveTo().equals(Direction.NONE));
    assertTrue(inner.isDisruptiveTo().equals(Direction.SOUTH));
  }

  @Test
  @DisplayName("SNAP-C04 스냅샷은 생존·암세포 집합을 각각 방어 복사한다")
  void snapshotDefensivelyCopiesBothCellSets() {
    // Given
    CellPosition normal = new CellPosition(0, 0);
    CellPosition cancer = new CellPosition(63, 63);
    Set<CellPosition> liveCells = new HashSet<>(Set.of(normal, cancer));
    Set<CellPosition> cancerCells = new HashSet<>(Set.of(cancer));
    BoardSnapshot captured = new BoardSnapshot(BOARD_SIZE, BOARD_SIZE, liveCells, cancerCells);

    // When
    liveCells.clear();
    cancerCells.clear();

    // Then
    assertEquals(Set.of(normal, cancer), captured.liveCells());
    assertEquals(Set.of(cancer), captured.cancerCells());
    assertThrows(UnsupportedOperationException.class, () -> captured.liveCells().clear());
    assertThrows(UnsupportedOperationException.class, () -> captured.cancerCells().clear());
  }

  @ParameterizedTest(name = "row={0}, column={1}")
  @CsvSource({"-1, 0", "64, 0", "0, 64", "10, 11"})
  @DisplayName("SNAP-C05 생존 집합 밖 암세포 좌표는 복원 전에 거부한다")
  void rejectsCancerOutsideLiveCellsBeforeRestore(int row, int column) {
    // Given
    Neighborhood root = neighborhood();
    Board board = new CompositeBoard(root);
    CellPosition existing = new CellPosition(10, 10);
    board.toggle(existing);
    CancerModelTest.cellAt(root, existing.row(), existing.column()).setCancer();
    BoardSnapshot before = board.snapshot();

    // When / Then
    assertThrows(
        IllegalArgumentException.class,
        () ->
            board.restore(
                new BoardSnapshot(
                    BOARD_SIZE, BOARD_SIZE, Set.of(existing), Set.of(new CellPosition(row, column)))));
    assertEquals(before, board.snapshot());
  }

  @Test
  @DisplayName("SNAP-C06 크기가 다른 스냅샷은 기존 암세포를 지우지 않는다")
  void sizeMismatchKeepsExistingCancerCells() {
    // Given
    Neighborhood root = neighborhood();
    Board board = new CompositeBoard(root);
    board.toggle(new CellPosition(10, 10));
    CancerModelTest.cellAt(root, 10, 10).setCancer();
    BoardSnapshot before = board.snapshot();
    CellPosition other = new CellPosition(2, 2);
    BoardSnapshot differentSize =
        new BoardSnapshot(BOARD_SIZE - 1, BOARD_SIZE - 1, Set.of(other), Set.of(other));

    // When / Then
    assertThrows(IllegalArgumentException.class, () -> board.restore(differentSize));
    assertEquals(before, board.snapshot());
    assertTrue(CancerModelTest.cellAt(root, 10, 10).isCancer());
  }

  @Test
  @DisplayName("SNAP-C07 기존 3인자 스냅샷을 복원하면 지정된 일반 세포만 남는다")
  void classicSnapshotClearsPreviousCancerState() {
    // Given
    Neighborhood root = neighborhood();
    Board board = new CompositeBoard(root);
    board.toggle(new CellPosition(10, 10));
    CancerModelTest.cellAt(root, 10, 10).setCancer();
    BoardSnapshot classic =
        new BoardSnapshot(BOARD_SIZE, BOARD_SIZE, Set.of(new CellPosition(4, 4)));

    // When
    board.restore(classic);

    // Then
    assertTrue(classic.cancerCells().isEmpty());
    assertEquals(classic, board.snapshot());
    assertEquals(0, root.cancerCount());
    assertFalse(CancerModelTest.cellAt(root, 4, 4).isCancer());
  }

  @Test
  @DisplayName("SNAP-C08 빈 스냅샷을 복원하면 생존·암 상태가 모두 비워진다")
  void emptySnapshotRemovesAllCellStates() {
    // Given
    Neighborhood root = neighborhood();
    Board board = new CompositeBoard(root);
    BoardSnapshot empty = board.snapshot();
    board.toggle(new CellPosition(0, 0));
    CancerModelTest.cellAt(root, 0, 0).setCancer();

    // When
    board.restore(empty);

    // Then
    assertEquals(empty, board.snapshot());
    assertTrue(empty.liveCells().isEmpty());
    assertTrue(empty.cancerCells().isEmpty());
    assertEquals(0, root.cancerCount());
    assertFalse(board.step());
  }

  @Test
  @DisplayName("SNAP-C09 null 암세포 집합과 null 원소는 거부한다")
  void rejectsNullCancerCollectionsAndElements() {
    Set<CellPosition> liveCells = Set.of(new CellPosition(10, 10));
    Set<CellPosition> withNull = new HashSet<>();
    withNull.add(null);

    assertThrows(
        NullPointerException.class,
        () -> new BoardSnapshot(BOARD_SIZE, BOARD_SIZE, liveCells, null));
    assertThrows(
        NullPointerException.class,
        () -> new BoardSnapshot(BOARD_SIZE, BOARD_SIZE, liveCells, withNull));
  }

  private static Neighborhood neighborhood() {
    return new Neighborhood(8, new Neighborhood(8, new Resident()));
  }
}
