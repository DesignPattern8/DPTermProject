package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CancerModelTest {
  /** Random stand-in: every nextDouble/nextInt call returns the same value. */
  static final class FixedRandom extends Random {
    private final double nextDouble;
    private final int nextInt;

    FixedRandom(double nextDouble, int nextInt) {
      this.nextDouble = nextDouble;
      this.nextInt = nextInt;
    }

    @Override
    public double nextDouble() {
      return nextDouble;
    }

    @Override
    public int nextInt(int bound) {
      return nextInt;
    }
  }

  private Neighborhood board;

  @BeforeEach
  void setUp() {
    board = new Neighborhood(8, new Neighborhood(8, new Resident()));
  }

  static Cell cellAt(Neighborhood board, int row, int column) {
    Neighborhood block = (Neighborhood) board.childAt(row / 8, column / 8);
    return block.childAt(row % 8, column % 8);
  }

  private void cancerAt(int row, int column) {
    cellAt(board, row, column).setCancer();
  }

  private static Resident liveResident() {
    Resident cell = new Resident();
    cell.toggle(0, 0);
    return cell;
  }

  private static Resident cancerResident() {
    Resident cell = new Resident();
    cell.setCancer();
    return cell;
  }

  private static void step(Resident cell, Cell[] n) {
    cell.figureNextState(n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7]);
    cell.transition();
  }

  @ParameterizedTest(name = "liveNeighbors={0}")
  @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
  @DisplayName("CAN-M01 암세포는 살아 있는 이웃이 1~5개일 때만 암세포로 남는다")
  void cancerCellSurvivesWithOneToFiveLiveNeighbors(int liveNeighbors) {
    // Given
    Resident cell = cancerResident();
    Cell[] neighbors = new Cell[8];
    for (int i = 0; i < 8; i++) neighbors[i] = i < liveNeighbors ? liveResident() : Cell.DUMMY;

    // When
    step(cell, neighbors);

    // Then
    boolean survives = liveNeighbors >= 1 && liveNeighbors <= 5;
    assertEquals(survives, cell.isAlive());
    assertEquals(survives, cell.isCancer());
  }

  @ParameterizedTest(name = "cancerNeighbors={0}")
  @ValueSource(ints = {2, 3, 4, 5})
  @DisplayName("CAN-M02 빈칸 주변에 암세포가 3~4개면 암세포가 태어난다")
  void cancerIsBornNextToThreeOrFourCancerCells(int cancerNeighbors) {
    // Given
    Resident cell = new Resident();
    Cell[] neighbors = new Cell[8];
    for (int i = 0; i < 8; i++) neighbors[i] = i < cancerNeighbors ? cancerResident() : Cell.DUMMY;

    // When
    step(cell, neighbors);

    // Then
    boolean born = cancerNeighbors == 3 || cancerNeighbors == 4;
    assertEquals(born, cell.isAlive());
    assertEquals(born, cell.isCancer());
  }

  @Test
  @DisplayName("CAN-M03 암 발생은 시작 세포와 8방향으로 이어진 살아 있는 일반 세포만 바꾼다")
  void outbreakConvertsOnlyTheConnectedGroup() {
    // Given: L-shaped group starting at (10,10) and a distant single cell
    board.toggle(10, 10);
    board.toggle(10, 11);
    board.toggle(11, 12);
    board.toggle(30, 30);

    // When: first candidate in row-major order, i.e. (10,10)
    board.tryCancerOutbreak(1.0, new FixedRandom(0.0, 0));

    // Then
    assertTrue(cellAt(board, 10, 10).isCancer());
    assertTrue(cellAt(board, 10, 11).isCancer());
    assertTrue(cellAt(board, 11, 12).isCancer());
    assertTrue(cellAt(board, 30, 30).isAlive());
    assertFalse(cellAt(board, 30, 30).isCancer());
    assertEquals(3, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-M04 확률 판정에서 떨어지면 암이 발생하지 않는다")
  void outbreakDoesNothingWhenProbabilityCheckFails() {
    board.toggle(5, 5);

    board.tryCancerOutbreak(0.1, new FixedRandom(0.5, 0));

    assertEquals(0, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-M05 빈 보드에서는 암 발생·자연 발생이 아무것도 바꾸지 않는다")
  void outbreakOnEmptyBoardDoesNothing() {
    Random random = new FixedRandom(0.0, 0);

    board.tryCancerOutbreak(1.0, random);
    board.tryNaturalCancer(1.0, random);

    assertEquals(0, board.cancerCount());
    assertTrue(board.createMemento().liveCells().isEmpty());
  }

  @Test
  @DisplayName("CAN-M06 자연 발생은 살아 있는 일반 세포 하나만 암세포로 바꾼다")
  void naturalCancerConvertsExactlyOneCell() {
    board.toggle(5, 5);
    board.toggle(5, 6);
    board.toggle(20, 20);

    board.tryNaturalCancer(1.0, new FixedRandom(0.0, 0));

    assertEquals(1, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-M07 전이는 암세포에 붙어 있는 일반 세포만, 한 세대에 한 칸씩만 감염시킨다")
  void invasionInfectsOnlyDirectNeighborsInOneGeneration() {
    cancerAt(20, 20);
    board.toggle(20, 21);
    board.toggle(20, 22);

    board.applyInvasion(1.0, new FixedRandom(0.0, 0));

    assertTrue(cellAt(board, 20, 21).isCancer());
    assertTrue(cellAt(board, 20, 22).isAlive());
    assertFalse(cellAt(board, 20, 22).isCancer());
  }

  @Test
  @DisplayName("CAN-M08 전이 확률이 0이면 감염되지 않는다")
  void invasionWithZeroProbabilityInfectsNothing() {
    cancerAt(20, 20);
    board.toggle(20, 21);

    board.applyInvasion(0.0, new FixedRandom(0.0, 0));

    assertFalse(cellAt(board, 20, 21).isCancer());
    assertEquals(1, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-M09 치료는 클릭한 칸 중심 3×3 안의 암세포만 없애고 일반 세포는 남긴다")
  void treatmentCuresOnlyCancerInThreeByThreeArea() {
    cancerAt(29, 29);
    cancerAt(31, 31);
    board.toggle(30, 31);
    cancerAt(33, 33);

    board.treatAt(30, 30);

    assertFalse(cellAt(board, 29, 29).isAlive());
    assertFalse(cellAt(board, 31, 31).isAlive());
    assertTrue(cellAt(board, 30, 31).isAlive());
    assertTrue(cellAt(board, 33, 33).isCancer());
  }

  @Test
  @DisplayName("CAN-M10 보드 모서리 치료는 보드 안쪽 칸만 처리한다")
  void treatmentAtCornerStaysInsideBoard() {
    cancerAt(0, 0);
    cancerAt(1, 1);

    assertDoesNotThrow(() -> board.treatAt(0, 0));

    assertEquals(0, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-M11 보드 밖 좌표 치료는 아무것도 바꾸지 않는다")
  void treatmentOutsideBoardChangesNothing() {
    cancerAt(0, 0);

    assertDoesNotThrow(() -> board.treatAt(-5, -5));
    assertDoesNotThrow(() -> board.treatAt(100, 100));

    assertEquals(1, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-M12 암세포 수는 하위 블록 경계를 넘어 모두 센다")
  void cancerCountSpansSubBlocks() {
    cancerAt(7, 7);
    cancerAt(7, 8);
    cancerAt(8, 7);
    cancerAt(8, 8);
    cancerAt(63, 63);

    assertEquals(5, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-M13 Clear는 암세포 상태도 지운다")
  void clearRemovesCancer() {
    cancerAt(3, 3);
    cancerAt(40, 40);

    board.clear();

    assertEquals(0, board.cancerCount());
    assertTrue(board.createMemento().liveCells().isEmpty());
  }

  @Test
  @DisplayName("CAN-M14 복합 셀과 DUMMY는 암세포가 되지 않는다")
  void compositeAndDummyCellsAreNeverCancer() {
    board.setCancer();
    Cell.DUMMY.setCancer();

    assertFalse(board.isCancer());
    assertFalse(Cell.DUMMY.isCancer());
  }
}
