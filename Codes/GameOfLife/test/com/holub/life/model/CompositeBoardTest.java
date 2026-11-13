package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompositeBoardTest {
  private static final int BOARD_SIZE = 64;

  @Test
  @DisplayName("BOARD-01 한 세대를 진행하고 생사 변경 여부를 반환한다")
  void stepReportsWhetherCellStatesChanged() {
    Board blinker =
        boardWith(new CellPosition(10, 9), new CellPosition(10, 10), new CellPosition(10, 11));
    Board block =
        boardWith(
            new CellPosition(20, 20),
            new CellPosition(20, 21),
            new CellPosition(21, 20),
            new CellPosition(21, 21));
    BoardSnapshot stable = block.snapshot();

    assertTrue(blinker.step());
    assertEquals(
        Set.of(new CellPosition(9, 10), new CellPosition(10, 10), new CellPosition(11, 10)),
        blinker.snapshot().liveCells());
    assertFalse(block.step());
    assertEquals(stable, block.snapshot());
  }

  @Test
  @DisplayName("BOARD-02 스냅샷을 복원하면 현재 배치 전체가 교체된다")
  void restoreReplacesTheEntireBoard() {
    CellPosition first = new CellPosition(2, 11);
    CellPosition second = new CellPosition(13, 24);
    Board board = boardWith(first, second);
    BoardSnapshot captured = board.snapshot();

    board.clear();
    assertTrue(board.snapshot().liveCells().isEmpty());
    board.toggle(new CellPosition(32, 8));
    board.restore(captured);

    assertEquals(Set.of(first, second), board.snapshot().liveCells());
  }

  @Test
  @DisplayName("BOARD-03 오른쪽 위 모서리를 행·열 순서로 캡처하고 편집 뒤에도 유지한다")
  void snapshotKeepsTopRightPositionAfterEditingBoard() {
    CellPosition topRight = new CellPosition(0, BOARD_SIZE - 1);
    Board board = boardWith(topRight);
    assertEquals(BOARD_SIZE, board.rows());
    assertEquals(BOARD_SIZE, board.columns());
    BoardSnapshot captured = board.snapshot();

    board.toggle(topRight);

    assertEquals(new BoardSnapshot(BOARD_SIZE, BOARD_SIZE, Set.of(topRight)), captured);
    assertTrue(board.snapshot().liveCells().isEmpty());
  }

  @Test
  @DisplayName("BOARD-04 복원 후 첫 세대에 하위 블록 경계 너머에서도 셀이 태어난다")
  void restoredBlinkerCrossesNeighborhoodBoundaryOnFirstStep() {
    Set<CellPosition> horizontal =
        Set.of(new CellPosition(7, 2), new CellPosition(7, 3), new CellPosition(7, 4));
    Board directlyEdited = boardWith();
    horizontal.forEach(directlyEdited::toggle);
    Board restored = boardWith();
    restored.restore(new BoardSnapshot(BOARD_SIZE, BOARD_SIZE, horizontal));

    directlyEdited.step();
    restored.step();

    BoardSnapshot restoredNext = restored.snapshot();
    assertEquals(directlyEdited.snapshot(), restoredNext);
    assertEquals(
        Set.of(new CellPosition(6, 3), new CellPosition(7, 3), new CellPosition(8, 3)),
        restoredNext.liveCells());
  }

  @Test
  @DisplayName("BOARD-05 스냅샷은 원본 집합의 변경과 반환 집합을 통한 변경을 막는다")
  void snapshotDefensivelyCopiesLiveCells() {
    CellPosition original = new CellPosition(3, 4);
    Set<CellPosition> source = new HashSet<>();
    source.add(original);
    BoardSnapshot captured = new BoardSnapshot(BOARD_SIZE, BOARD_SIZE, source);

    source.clear();

    assertEquals(Set.of(original), captured.liveCells());
    assertThrows(
        UnsupportedOperationException.class, () -> captured.liveCells().add(new CellPosition(4, 3)));
  }

  @Test
  @DisplayName("BOARD-06 범위 밖 편집을 거부하고 기존 배치를 유지한다")
  void toggleRejectsOutOfBoundsPositionsWithoutChangingBoard() {
    Board board = boardWith(new CellPosition(3, 5));
    BoardSnapshot before = board.snapshot();

    for (CellPosition invalid :
        Set.of(
            new CellPosition(-1, 0),
            new CellPosition(0, -1),
            new CellPosition(BOARD_SIZE, 0),
            new CellPosition(0, BOARD_SIZE))) {
      assertThrows(IndexOutOfBoundsException.class, () -> board.toggle(invalid));
      assertEquals(before, board.snapshot());
    }
  }

  @Test
  @DisplayName("BOARD-07 크기가 다른 스냅샷의 복원을 거부하고 기존 배치를 유지한다")
  void restoreRejectsSizeMismatchWithoutChangingBoard() {
    Board board = boardWith(new CellPosition(3, 5));
    BoardSnapshot before = board.snapshot();
    BoardSnapshot differentSize =
        new BoardSnapshot(BOARD_SIZE - 1, BOARD_SIZE - 1, Set.of(new CellPosition(2, 1)));

    assertThrows(IllegalArgumentException.class, () -> board.restore(differentSize));
    assertEquals(before, board.snapshot());
  }

  @Test
  @DisplayName("BOARD-08 배치를 캡처해도 블록 경계의 활성 방향이 바뀌지 않는다")
  void snapshotDoesNotChangeActiveEdges() {
    Neighborhood neighborhood = new Neighborhood(8, new Neighborhood(8, new Resident()));
    Neighborhood inner = (Neighborhood) neighborhood.childAt(2, 2);
    Board board = new CompositeBoard(neighborhood);
    CellPosition edge = new CellPosition(23, 20);
    board.toggle(edge);
    assertTrue(neighborhood.isDisruptiveTo().equals(Direction.NONE));
    assertTrue(inner.isDisruptiveTo().equals(Direction.SOUTH));

    assertEquals(Set.of(edge), board.snapshot().liveCells());

    assertTrue(neighborhood.isDisruptiveTo().equals(Direction.NONE));
    assertTrue(inner.isDisruptiveTo().equals(Direction.SOUTH));
  }

  @Test
  @DisplayName("BOARD-09 잘못된 스냅샷 크기나 좌표를 거부한다")
  void snapshotRejectsInvalidDimensionsAndCoordinates() {
    assertThrows(IllegalArgumentException.class, () -> new BoardSnapshot(0, BOARD_SIZE, Set.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new BoardSnapshot(BOARD_SIZE, BOARD_SIZE, Set.of(new CellPosition(0, BOARD_SIZE))));
  }

  private static Board boardWith(CellPosition... liveCells) {
    Board board = new CompositeBoard(new Neighborhood(8, new Neighborhood(8, new Resident())));
    for (CellPosition position : liveCells) {
      board.toggle(position);
    }
    return board;
  }
}
