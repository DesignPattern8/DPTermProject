package com.holub.life;

import com.holub.life.model.Neighborhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 클래식 모드 세대 진행 특성 테스트 (docs/test-cases.md CL-G). */
class ClassicGenerationTest {
  private static final int[][] BLOCK = {{10, 10}, {10, 11}, {11, 10}, {11, 11}};
  private static final int[][] BLINKER = {{10, 9}, {10, 10}, {10, 11}};
  private static final int[][] GLIDER = {{1, 2}, {2, 3}, {3, 1}, {3, 2}, {3, 3}};

  /** board를 generations번 tick하면서 매 세대 단순 구현과 같은지 확인한다. */
  private static void assertMatchesReference(ClassicBoard board, int generations) {
    ReferenceLife reference = new ReferenceLife(ClassicBoard.SIZE, board.liveCells());
    for (int g = 1; g <= generations; g++) {
      board.tick();
      reference.tick();
      assertEquals(reference.liveCells(), board.liveCells(), "세대 " + g);
    }
  }

  @Test
  @DisplayName("CL-G-01 정물(block)은 tick을 해도 변하지 않는다")
  void blockIsStill() {
    // Given
    ClassicBoard board = new ClassicBoard().clickAll(BLOCK);
    // When
    board.tick(5);
    // Then
    assertEquals(ClassicBoard.cells(BLOCK), board.liveCells());
  }

  @Test
  @DisplayName("CL-G-02 blinker는 2세대마다 원래 모양으로 돌아온다")
  void blinkerReturnsEveryTwoGenerations() {
    // Given
    ClassicBoard board = new ClassicBoard().clickAll(BLINKER);
    // When
    board.tick();
    Set<Point> afterOne = board.liveCells();
    board.tick();
    // Then
    assertEquals(ClassicBoard.cells(new int[][] {{9, 10}, {10, 10}, {11, 10}}), afterOne);
    assertEquals(ClassicBoard.cells(BLINKER), board.liveCells());
  }

  @Test
  @DisplayName("CL-G-03 glider는 4세대 뒤 오른쪽 아래로 1칸 이동한다")
  void gliderMovesDiagonallyEveryFourGenerations() {
    // Given
    ClassicBoard board = new ClassicBoard().clickAll(GLIDER);
    // When
    board.tick(4);
    // Then
    assertEquals(ClassicBoard.shifted(ClassicBoard.cells(GLIDER), 1, 1), board.liveCells());
  }

  @Test
  @DisplayName("CL-G-04 빈 보드는 tick을 해도 빈 보드다")
  void emptyBoardStaysEmpty() {
    // Given
    ClassicBoard board = new ClassicBoard();
    // When
    board.tick(3);
    // Then
    assertTrue(board.liveCells().isEmpty());
  }

  @Test
  @DisplayName("CL-G-05 보드 모서리에서는 바깥을 죽은 셀로 본다")
  void cornerTreatsOutsideAsDead() {
    // Given
    ClassicBoard board = new ClassicBoard().clickAll(new int[][] {{0, 1}, {1, 0}, {1, 1}});
    // When
    board.tick();
    // Then
    assertEquals(ClassicBoard.cells(new int[][] {{0, 0}, {0, 1}, {1, 0}, {1, 1}}), board.liveCells());
  }

  @Test
  @DisplayName("CL-G-06 glider가 하위 Neighborhood(8칸) 경계를 넘어도 모양이 유지된다")
  void gliderKeepsShapeAcrossNeighborhoodBoundary() {
    // Given
    ClassicBoard board = new ClassicBoard().clickAll(GLIDER);
    // When
    board.tick(32);
    // Then
    assertEquals(ClassicBoard.shifted(ClassicBoard.cells(GLIDER), 8, 8), board.liveCells());
  }

  @Test
  @DisplayName("CL-G-07 하위 Neighborhood의 가장자리 한 행 전체가 동시에 바뀌어도 결과가 단순 구현과 같다")
  void fullNeighborhoodEdgeRowMatchesReference() {
    // Given
    int[][] row = new int[8][];
    for (int c = 0; c < 8; c++) row[c] = new int[] {7, c};
    ClassicBoard board = new ClassicBoard().clickAll(row);
    // When / Then
    assertMatchesReference(board, 30);
  }

  @Test
  @DisplayName("CL-G-08 64×64 무작위 보드(시드 42) 5개가 100세대 동안 단순 구현과 같다")
  void randomBoardsMatchReference() {
    Random random = new Random(42);
    for (int trial = 0; trial < 5; trial++) {
      // Given
      List<int[]> cells = new ArrayList<>();
      for (int r = 0; r < ClassicBoard.SIZE; r++)
        for (int c = 0; c < ClassicBoard.SIZE; c++) if (random.nextInt(4) == 0) cells.add(new int[] {r, c});
      ClassicBoard board = new ClassicBoard().clickAll(cells.toArray(new int[0][]));
      // When / Then
      assertMatchesReference(board, 100);
    }
  }

  @Test
  @DisplayName("CL-G-09 R-pentomino가 300세대 동안 단순 구현과 같다")
  void rPentominoMatchesReference() {
    // Given
    ClassicBoard board =
        new ClassicBoard().clickAll(new int[][] {{30, 31}, {30, 32}, {31, 30}, {31, 31}, {32, 31}});
    // When / Then
    assertMatchesReference(board, 300);
  }

  @Test
  @DisplayName("CL-G-10 glider가 보드 가장자리에 닿은 뒤에도 240세대 동안 단순 구현과 같다")
  void gliderHittingBoardEdgeMatchesReference() {
    // Given
    ClassicBoard board = new ClassicBoard().clickAll(GLIDER);
    // When / Then
    assertMatchesReference(board, 240);
  }
}
