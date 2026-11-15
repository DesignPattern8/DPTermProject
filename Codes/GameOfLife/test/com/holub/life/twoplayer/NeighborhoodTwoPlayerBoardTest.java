package com.holub.life.twoplayer;

import static com.holub.life.twoplayer.RealBoardScript.advance;
import static com.holub.life.twoplayer.RealBoardScript.at;
import static com.holub.life.twoplayer.RealBoardScript.cells;
import static com.holub.life.twoplayer.RealBoardScript.counts;
import static com.holub.life.twoplayer.RealBoardScript.liveCells;
import static com.holub.life.twoplayer.RealBoardScript.place;
import static com.holub.life.twoplayer.RealBoardScript.with;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TwoPlayerBoard 계약 (docs/test-cases.md 1-2장 TP-B). 계약 위반 시 동작(예외)은 test-cases.md 1-2장에서 정했다.
 */
class NeighborhoodTwoPlayerBoardTest {
  private static final Player A = Player.A;
  private static final Player B = Player.B;
  private static final int[][] OUT_OF_BOARD = {{-1, 0}, {0, -1}, {64, 0}, {0, 64}};

  private final NeighborhoodTwoPlayerBoard board = new NeighborhoodTwoPlayerBoard();

  /** 0~31열 A, 32~63열 B. skipLast면 (63,63)만 비워 둔다. */
  private void fill(boolean skipLast) {
    for (int r = 0; r < 64; r++) {
      for (int c = 0; c < 64; c++) {
        if (skipLast && r == 63 && c == 63) {
          continue;
        }
        board.place(r, c, c < 32 ? A : B);
      }
    }
  }

  @Test
  @DisplayName("TP-B-01 기본 보드는 64×64다")
  void defaultBoardIs64By64() {
    // Given: 새 보드
    // When / Then
    assertAll(() -> assertEquals(64, board.rows()), () -> assertEquals(64, board.columns()));
  }

  @Test
  @DisplayName("TP-B-02 새 보드는 모든 칸이 비어 있다")
  void newBoardIsEmpty() {
    // Given: 새 보드
    // When / Then
    for (int r = 0; r < 64; r++) {
      for (int c = 0; c < 64; c++) {
        assertTrue(board.isEmpty(r, c), "(" + r + "," + c + ")");
      }
    }
    assertArrayEquals(new int[] {0, 0}, counts(board));
  }

  @Test
  @DisplayName("TP-B-03 place한 칸만 비지 않게 되고 그 소유자 수가 1 늘어난다")
  void placeFillsOnlyThatCell() {
    // When
    board.place(10, 10, A);
    // Then
    assertFalse(board.isEmpty(10, 10));
    for (int dr = -1; dr <= 1; dr++) {
      for (int dc = -1; dc <= 1; dc++) {
        if (dr != 0 || dc != 0) {
          assertTrue(board.isEmpty(10 + dr, 10 + dc), "neighbor " + dr + "," + dc);
        }
      }
    }
    assertArrayEquals(new int[] {1, 0}, counts(board));
  }

  @Test
  @DisplayName("TP-B-04 B도 같은 방식으로 놓인다")
  void placeCountsForOwnerB() {
    // When
    board.place(10, 50, B);
    // Then
    assertFalse(board.isEmpty(10, 50));
    assertArrayEquals(new int[] {0, 1}, counts(board));
  }

  @Test
  @DisplayName("TP-B-05 보드는 진영을 검사하지 않는다 (진영 제한은 게임 진행의 일)")
  void boardDoesNotCheckHalves() {
    // When: B 진영에 A
    board.place(10, 40, A);
    // Then
    assertFalse(board.isEmpty(10, 40));
    assertArrayEquals(new int[] {1, 0}, counts(board));
  }

  @Test
  @DisplayName("TP-B-06 세대 진행으로 생기고 죽은 칸이 isEmpty에 반영된다")
  void isEmptyReflectsBirthAndDeath() {
    // Given: TP-R-01 배치 (명세 예시 1)
    place(board, A, at(1, 1), at(2, 2));
    place(board, B, at(3, 1));
    // When
    board.advanceGeneration();
    // Then: (2,1) 탄생, (3,1) 고립사
    assertFalse(board.isEmpty(2, 1));
    assertTrue(board.isEmpty(3, 1));
  }

  @Test
  @DisplayName("TP-B-07 정체한 보드에 place한 셀도 다음 세대 계산에 들어간다")
  void placeOnQuietBoardJoinsNextGeneration() {
    // Given
    advance(board, 5);
    place(board, A, at(20, 20), at(20, 21), at(20, 22));
    // When
    board.advanceGeneration();
    // Then
    assertEquals(cells(A, at(19, 21), at(20, 21), at(21, 21)), liveCells(board));
  }

  @Test
  @DisplayName("TP-B-08 네 모서리 칸에도 놓을 수 있다")
  void placeOnFourCorners() {
    // When
    place(board, A, at(0, 0), at(63, 0));
    place(board, B, at(0, 63), at(63, 63));
    // Then
    assertEquals(
        with(cells(A, at(0, 0), at(63, 0)), B, at(0, 63), at(63, 63)), liveCells(board));
    assertArrayEquals(new int[] {2, 2}, counts(board));
  }

  @Test
  @DisplayName("TP-B-09 빈 칸이 있으면 hasEmptyCell은 true다")
  void hasEmptyCellOnNewBoard() {
    // Given: 새 보드
    // When / Then
    assertTrue(board.hasEmptyCell());
  }

  @Test
  @DisplayName("TP-B-10 빈 칸이 딱 하나 남아도 hasEmptyCell은 true, 다 차면 false다")
  void hasEmptyCellUntilLastCellFilled() {
    // Given: (63,63)만 빼고 4,095칸
    fill(true);
    // Then
    assertTrue(board.hasEmptyCell());
    assertArrayEquals(new int[] {2048, 2047}, counts(board));
    // When
    board.place(63, 63, B);
    // Then
    assertFalse(board.hasEmptyCell());
    assertArrayEquals(new int[] {2048, 2048}, counts(board));
  }

  @Test
  @DisplayName("TP-B-11 가득 찬 보드는 1세대 뒤 네 모서리만 남는다")
  void fullBoardLeavesOnlyCorners() {
    // Given
    fill(false);
    // When
    board.advanceGeneration();
    // Then: 모서리 이웃 3(생존), 가장자리 5·안쪽 8(과밀사)
    assertEquals(
        with(cells(A, at(0, 0), at(63, 0)), B, at(0, 63), at(63, 63)), liveCells(board));
    assertTrue(board.hasEmptyCell());
    // When
    board.advanceGeneration();
    // Then
    assertArrayEquals(new int[] {0, 0}, counts(board));
  }

  @Test
  @DisplayName("TP-B-12 빈 보드의 세대 진행은 아무것도 바꾸지 않는다")
  void emptyBoardStaysEmpty() {
    // When
    board.advanceGeneration();
    // Then
    assertArrayEquals(new int[] {0, 0}, counts(board));
    assertTrue(board.hasEmptyCell());
  }

  @Test
  @DisplayName("TP-B-13 범위 밖 좌표의 isEmpty는 IndexOutOfBoundsException을 던진다")
  void isEmptyRejectsOutOfBoard() {
    for (int[] p : OUT_OF_BOARD) {
      // When / Then
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> board.isEmpty(p[0], p[1]),
          "(" + p[0] + "," + p[1] + ")");
    }
  }

  @Test
  @DisplayName("TP-B-14 범위 밖 좌표의 place는 IndexOutOfBoundsException을 던지고 보드를 바꾸지 않는다")
  void placeRejectsOutOfBoard() {
    for (int[] p : OUT_OF_BOARD) {
      // When / Then
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> board.place(p[0], p[1], A),
          "(" + p[0] + "," + p[1] + ")");
    }
    assertArrayEquals(new int[] {0, 0}, counts(board));
  }

  @Test
  @DisplayName("TP-B-15 살아 있는 칸에는 place할 수 없고(IllegalArgumentException), 그 셀은 그대로다")
  void placeOnLiveCellIsRejectedAndKeepsCell() {
    // Given
    board.place(10, 10, A);
    // When / Then
    assertThrows(IllegalArgumentException.class, () -> board.place(10, 10, A));
    assertThrows(IllegalArgumentException.class, () -> board.place(10, 10, B));
    assertEquals(cells(A, at(10, 10)), liveCells(board));
    assertArrayEquals(new int[] {1, 0}, counts(board));
  }

  @Test
  @DisplayName("TP-B-16 소유자 없이 place할 수 없다 (NullPointerException)")
  void placeRejectsNullOwner() {
    // When / Then
    assertThrows(NullPointerException.class, () -> board.place(10, 10, null));
    assertTrue(board.isEmpty(10, 10));
    assertArrayEquals(new int[] {0, 0}, counts(board));
  }

  @Test
  @DisplayName("TP-B-17 소유자 없이 count할 수 없다 (NullPointerException, DEAD 칸 수를 돌려주지 않음)")
  void countRejectsNullOwner() {
    // When / Then
    assertThrows(NullPointerException.class, () -> board.count(null));
  }
}
