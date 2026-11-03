package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 클래식 모드 마우스 토글·Clear 특성 테스트 (docs/test-cases.md CL-E). */
class ClassicEditTest {
  private static final int[][] BLINKER = {{10, 9}, {10, 10}, {10, 11}};
  private static final int[][] BLOCK = {{30, 30}, {30, 31}, {31, 30}, {31, 31}};

  @Test
  @DisplayName("CL-E-01 죽은 칸을 클릭하면 살아난다")
  void clickRevivesDeadCell() {
    // Given
    ClassicBoard board = new ClassicBoard();
    // When
    board.click(5, 7);
    // Then
    assertEquals(ClassicBoard.cells(new int[][] {{5, 7}}), board.liveCells());
  }

  @Test
  @DisplayName("CL-E-02 산 칸을 다시 클릭하면 죽는다")
  void clickTwiceKillsCell() {
    // Given
    ClassicBoard board = new ClassicBoard().click(5, 7);
    // When
    board.click(5, 7);
    // Then
    assertTrue(board.liveCells().isEmpty());
  }

  @Test
  @DisplayName("CL-E-03 보드 네 모서리 칸도 클릭할 수 있다")
  void cornerCellsAreClickable() {
    // Given
    int[][] corners = {{0, 0}, {0, 63}, {63, 0}, {63, 63}};
    ClassicBoard board = new ClassicBoard();
    // When
    board.clickAll(corners);
    // Then
    assertEquals(ClassicBoard.cells(corners), board.liveCells());
  }

  @Test
  @DisplayName("CL-E-04 클릭으로 만든 패턴은 다음 tick부터 규칙대로 진행한다")
  void clickedPatternEvolvesOnTick() {
    // Given
    ClassicBoard board = new ClassicBoard().clickAll(BLINKER);
    // When
    board.tick();
    // Then
    assertEquals(ClassicBoard.cells(new int[][] {{9, 10}, {10, 10}, {11, 10}}), board.liveCells());
  }

  @Test
  @DisplayName("CL-E-05 Clear는 모든 칸을 죽인다")
  void clearKillsAllCells() {
    // Given
    ClassicBoard board = new ClassicBoard().clickAll(BLINKER).clickAll(BLOCK);
    // When
    board.clear();
    // Then
    assertTrue(board.liveCells().isEmpty());
  }

  @Test
  @DisplayName("CL-E-06 Clear 뒤 tick을 해도 아무것도 살아나지 않는다")
  void nothingRevivesAfterClear() {
    // Given
    ClassicBoard board = new ClassicBoard().clickAll(BLINKER).clear();
    // When
    board.tick();
    // Then
    assertTrue(board.liveCells().isEmpty());
  }

  @Test
  @DisplayName("CL-E-07 [현재 동작] 보드 밖 좌표로 클릭하면 ArrayIndexOutOfBoundsException이 난다")
  void clickOutsideBoardThrows() {
    // Given
    ClassicBoard board = new ClassicBoard();
    // When / Then
    assertThrows(ArrayIndexOutOfBoundsException.class, () -> board.click(64, 0));
    assertThrows(ArrayIndexOutOfBoundsException.class, () -> board.click(-1, 0));
  }
}
