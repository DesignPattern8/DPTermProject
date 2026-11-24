package com.holub.life.twoplayer;

import static com.holub.life.twoplayer.RealBoardScript.advance;
import static com.holub.life.twoplayer.RealBoardScript.at;
import static com.holub.life.twoplayer.RealBoardScript.cells;
import static com.holub.life.twoplayer.RealBoardScript.counts;
import static com.holub.life.twoplayer.RealBoardScript.liveCells;
import static com.holub.life.twoplayer.RealBoardScript.place;
import static com.holub.life.twoplayer.RealBoardScript.with;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.holub.life.model.CancerCell;
import com.holub.life.model.CellPosition;
import com.holub.life.model.CompositeBoard;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 세대 전이 규칙 (docs/test-cases.md 1-1장 TP-R). 64×64 {@link NeighborhoodTwoPlayerBoard}로 확인한다. 기대값 출처는
 * test-cases.md 표의 "출처" 열.
 */
class TwoPlayerRuleTest {
  private static final Player A = Player.A;
  private static final Player B = Player.B;

  private final NeighborhoodTwoPlayerBoard board = new NeighborhoodTwoPlayerBoard();

  @Test
  @DisplayName("CAN-M14 2인용 셀은 암세포 계약을 제공하지 않는다")
  void twoPlayerCellsDoNotSupportCancer() {
    assertFalse(CancerCell.class.isAssignableFrom(TwoPlayerResident.class));
  }

  /** TP-R-01·07: 명세 예시 1. */
  private void placeExample1() {
    place(board, A, at(1, 1), at(2, 2));
    place(board, B, at(3, 1));
  }

  /** TP-R-13·14: 명세 예시 4. */
  private void placeExample4() {
    place(board, A, at(0, 1), at(1, 2), at(2, 0), at(2, 1), at(2, 2));
    place(board, B, at(5, 5), at(5, 6), at(6, 5), at(6, 6));
  }

  /** TP-R-16·17: 명세 예시 5. */
  private void placeExample5() {
    place(board, B, at(7, 7), at(7, 8), at(8, 7), at(8, 8));
    place(board, A, at(8, 9));
  }

  @Test
  @DisplayName("TP-R-01 이웃 3개 중 A가 2개면 A로 태어난다 (명세 예시 1)")
  void bornAsMajorityColorA() {
    // Given
    placeExample1();
    // When
    board.advanceGeneration();
    // Then
    assertEquals(cells(A, at(2, 1), at(2, 2)), liveCells(board));
  }

  @Test
  @DisplayName("TP-R-02 이웃 3개 중 B가 2개면 B로 태어난다 (TP-R-01의 색 반전)")
  void bornAsMajorityColorB() {
    // Given
    place(board, B, at(1, 1), at(2, 2));
    place(board, A, at(3, 1));
    // When
    board.advanceGeneration();
    // Then
    assertEquals(cells(B, at(2, 1), at(2, 2)), liveCells(board));
  }

  @Test
  @DisplayName("TP-R-03 상대 셀이 몰리면 내 셀이 과밀로 죽는다 (명세 예시 2)")
  void opponentNeighborsCauseOvercrowding() {
    // Given
    place(board, A, at(1, 1), at(1, 3), at(2, 2));
    place(board, B, at(1, 2), at(3, 2));
    // When
    board.advanceGeneration();
    // Then: (2,2) 과밀사, (0,2)에 A 탄생
    assertEquals(
        with(cells(A, at(0, 2), at(1, 1), at(1, 3)), B, at(1, 2)), liveCells(board));
  }

  @Test
  @DisplayName("TP-R-04 살아 있는 셀은 이웃이 모두 상대 색이어도 자기 색을 유지한다")
  void survivingCellKeepsItsColor() {
    // Given
    place(board, B, at(1, 1), at(3, 3));
    place(board, A, at(2, 2));
    // When
    board.advanceGeneration();
    // Then
    assertEquals(cells(A, at(2, 2)), liveCells(board));
  }

  @Test
  @DisplayName("TP-R-05 같은 색 3개로 태어난 셀은 그 색이다")
  void bornFromThreeSameColor() {
    // Given
    place(board, A, at(1, 1), at(1, 2), at(1, 3));
    // When
    board.advanceGeneration();
    // Then: 세로 blinker
    assertEquals(cells(A, at(0, 2), at(1, 2), at(2, 2)), liveCells(board));
  }

  @Test
  @DisplayName("TP-R-06 색을 가진 진동자도 주기마다 같은 색으로 돌아온다")
  void coloredBlinkerKeepsColorEachPeriod() {
    // Given
    place(board, A, at(10, 10), at(10, 11), at(10, 12));
    Map<Coordinate, Player> generation0 = liveCells(board);
    // When
    board.advanceGeneration();
    Map<Coordinate, Player> generation1 = liveCells(board);
    board.advanceGeneration();
    // Then
    assertEquals(cells(A, at(9, 11), at(10, 11), at(11, 11)), generation1);
    assertEquals(generation0, liveCells(board));
  }

  @Test
  @DisplayName("TP-R-07 이웃이 1개 이하면 색과 상관없이 고립사한다")
  void isolatedCellDies() {
    // Given: B(3,1)의 이웃은 A(2,2) 1개
    placeExample1();
    // When
    board.advanceGeneration();
    // Then
    assertTrue(board.isEmpty(3, 1));
  }

  @Test
  @DisplayName("TP-R-08 죽은 칸은 이웃이 4개면 태어나지 않는다")
  void noBirthWithFourNeighbors() {
    // Given: (2,2)는 DEAD이고 이웃 A 2 + B 2
    place(board, A, at(1, 1), at(1, 3));
    place(board, B, at(3, 1), at(3, 3));
    // When
    board.advanceGeneration();
    // Then: (2,2)는 태어나지 않고 네 셀도 고립사
    assertEquals(Map.of(), liveCells(board));
  }

  @Test
  @DisplayName("TP-R-09 보드 모서리에서는 바깥을 DEAD로 본다")
  void cornerTreatsOutsideAsDead() {
    // Given
    place(board, A, at(0, 1), at(1, 0), at(1, 1));
    // When
    board.advanceGeneration();
    // Then: (0,0)에 A 탄생, 2×2 block
    assertEquals(cells(A, at(0, 0), at(0, 1), at(1, 0), at(1, 1)), liveCells(board));
  }

  @Test
  @DisplayName("TP-R-10 하위 Neighborhood 경계(8칸 단위)를 넘는 칸에서도 탄생 색이 맞다")
  void birthAcrossNeighborhoodBoundary() {
    // Given: (8,8)은 네 블록이 만나는 칸
    place(board, A, at(7, 7), at(7, 8));
    place(board, B, at(8, 7));
    // When
    board.advanceGeneration();
    // Then
    assertEquals(Optional.of(A), board.owner(8, 8));
  }

  @Test
  @DisplayName("TP-R-11 한 번도 활성화된 적 없는 Neighborhood도 이웃 블록 가장자리의 변화로 다시 계산된다")
  void quietNeighborhoodWakesOnEdgeBirth() {
    // Given: 7열은 블록 (1,0)의 동쪽 가장자리, (10,8)은 아무것도 놓지 않은 블록 (1,1)
    place(board, A, at(9, 7), at(10, 7), at(11, 7));
    Map<Coordinate, Player> horizontal = cells(A, at(10, 6), at(10, 7), at(10, 8));
    Map<Coordinate, Player> vertical = cells(A, at(9, 7), at(10, 7), at(11, 7));
    for (int generation = 1; generation <= 6; generation++) {
      // When
      board.advanceGeneration();
      // Then: 홀수 세대에 (10,8) A 탄생, 짝수 세대에 세로로 돌아옴
      assertEquals(
          generation % 2 == 1 ? horizontal : vertical,
          liveCells(board),
          "generation " + generation);
    }
  }

  @Test
  @DisplayName("TP-R-12 서로 닿지 않는 진동자와 정물은 영원히 같은 상태를 반복한다 (명세 예시 3)")
  void disjointPatternsRepeatForever() {
    // Given
    place(board, A, at(2, 1), at(2, 2), at(2, 3));
    place(board, B, at(2, 6), at(2, 7), at(3, 6), at(3, 7));
    Map<Coordinate, Player> generation0 = liveCells(board);
    for (int generation = 0; generation <= 6; generation++) {
      // Then: 매 세대 A=3, B=4, 세대 2는 세대 0과 같음
      assertArrayEquals(new int[] {3, 4}, counts(board), "generation " + generation);
      if (generation == 2) {
        assertEquals(generation0, liveCells(board));
      }
      // When
      board.advanceGeneration();
    }
  }

  @Test
  @DisplayName("TP-R-13 glider와 block이 부딪히면 같은 세대에 둘 다 전멸한다 (명세 예시 4)")
  void gliderHitsBlockBothVanish() {
    // Given
    placeExample4();
    int[][] expected = {
      {5, 4}, {5, 4}, {5, 4}, {5, 4}, {5, 4}, {5, 4}, {5, 4}, {6, 5}, {3, 3}, {1, 2}, {0, 0}
    };
    for (int generation = 0; generation < expected.length; generation++) {
      // Then
      assertArrayEquals(expected[generation], counts(board), "generation " + generation);
      // When
      board.advanceGeneration();
    }
  }

  @Test
  @DisplayName("TP-R-14 섞인 이웃에서 다수색으로 태어나는 셀이 충돌 지점에 생긴다 (명세 예시 4)")
  void mixedNeighborsBirthAtCollision() {
    // Given
    placeExample4();
    // When
    advance(board, 7);
    // Then
    assertEquals(Optional.of(B), board.owner(5, 4));
  }

  @Test
  @DisplayName("TP-R-15 색을 지우면 클래식 B3/S23과 살아 있는 칸이 항상 같다 (불변조건)")
  void sameAliveCellsAsClassicWhenColorIgnored() {
    // Given: (a) TP-R-01·03·12·13의 배치 각 10세대, (b) 시드 42 무작위 보드 200세대
    List<Consumer<Placer>> examples =
        List.of(
            p -> {
              p.put(1, 1, A);
              p.put(2, 2, A);
              p.put(3, 1, B);
            },
            p -> {
              p.put(1, 1, A);
              p.put(1, 2, B);
              p.put(1, 3, A);
              p.put(2, 2, A);
              p.put(3, 2, B);
            },
            p -> {
              p.put(2, 1, A);
              p.put(2, 2, A);
              p.put(2, 3, A);
              p.put(2, 6, B);
              p.put(2, 7, B);
              p.put(3, 6, B);
              p.put(3, 7, B);
            },
            p -> {
              for (int[] c : new int[][] {{0, 1}, {1, 2}, {2, 0}, {2, 1}, {2, 2}}) {
                p.put(c[0], c[1], A);
              }
              for (int[] c : new int[][] {{5, 5}, {5, 6}, {6, 5}, {6, 6}}) {
                p.put(c[0], c[1], B);
              }
            });
    for (int i = 0; i < examples.size(); i++) {
      assertSameAliveCellsAsClassic("example " + i, examples.get(i), 10);
    }
    assertSameAliveCellsAsClassic(
        "random seed 42",
        p -> {
          Random random = new Random(42);
          for (int r = 0; r < 64; r++) {
            for (int c = 0; c < 64; c++) {
              if (random.nextDouble() < 0.3) {
                p.put(r, c, c < 32 ? A : B);
              }
            }
          }
        },
        200);
  }

  /** 같은 칸을 2인용 보드에는 place, 클래식 보드에는 toggle로 둔다. */
  private interface Placer {
    void put(int row, int column, Player owner);
  }

  private static void assertSameAliveCellsAsClassic(
      String name, Consumer<Placer> layout, int generations) {
    NeighborhoodTwoPlayerBoard twoPlayer = new NeighborhoodTwoPlayerBoard();
    CompositeBoard classic =
        new CompositeBoard(new Neighborhood(8, new Neighborhood(8, new Resident())));
    layout.accept(
        (row, column, owner) -> {
          twoPlayer.place(row, column, owner);
          classic.toggle(new CellPosition(row, column));
        });
    for (int generation = 0; generation <= generations; generation++) {
      // Then
      Set<CellPosition> twoPlayerAlive = new HashSet<>();
      for (Coordinate c : liveCells(twoPlayer).keySet()) {
        twoPlayerAlive.add(new CellPosition(c.row(), c.column()));
      }
      assertEquals(
          classic.snapshot().liveCells(), twoPlayerAlive, name + ", generation " + generation);
      // When
      twoPlayer.advanceGeneration();
      classic.step();
    }
  }

  @Test
  @DisplayName("TP-R-16 셀 1개 저격에 대응이 없으면 block이 무너진다 (명세 예시 5)")
  void snipeWithoutResponse() {
    // Given
    placeExample5();
    int[][] expected = {{1, 4}, {1, 4}, {1, 2}, {0, 2}, {0, 0}};
    for (int generation = 0; generation < expected.length; generation++) {
      // Then
      assertArrayEquals(expected[generation], counts(board), "generation " + generation);
      // When
      board.advanceGeneration();
    }
  }

  @Test
  @DisplayName("TP-R-17 (5,8) 대응을 두면 B가 살아남는다 (명세 예시 5)")
  void snipeWithResponseAt5_8() {
    // Given
    placeExample5();
    place(board, B, at(5, 8));
    int[] expectedB8to17 = {16, 32, 24, 40, 32, 40, 44, 56, 20, 24};
    // When
    advance(board, 7);
    // Then: A는 세대 7까지 살아 있고 세대 8부터 0
    assertTrue(board.count(A) > 0, "A at generation 7");
    for (int generation = 8; generation <= 30; generation++) {
      board.advanceGeneration();
      int expectedB = generation <= 17 ? expectedB8to17[generation - 8] : 24;
      assertArrayEquals(new int[] {0, expectedB}, counts(board), "generation " + generation);
    }
    assertEquals(
        cells(
            B,
            at(1, 7), at(2, 6), at(2, 8), at(3, 6), at(3, 8), at(4, 7),
            at(6, 2), at(6, 3), at(6, 11), at(6, 12), at(7, 1), at(7, 4),
            at(7, 10), at(7, 13), at(8, 2), at(8, 3), at(8, 11), at(8, 12),
            at(10, 7), at(11, 6), at(11, 8), at(12, 6), at(12, 8), at(13, 7)),
        liveCells(board));
  }
}
