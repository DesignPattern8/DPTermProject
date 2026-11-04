package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NeighborhoodRecomputationTest {
  @Test
  @DisplayName("CL-C-08 glider 하나만 움직일 때는 그 근처의 하위 블록만 다시 계산한다 (tick마다 Neighborhood 5개 이하)")
  void onlyBlocksNearChangesAreRecomputed() {
    // Given: 64×64 보드에 glider 하나, 첫 tick은 클릭으로 생긴 활성 표시를 소화한다
    Neighborhood board = new Neighborhood(8, new Neighborhood(8, new Resident()));
    for (int[] cell : new int[][] {{1, 2}, {2, 3}, {3, 1}, {3, 2}, {3, 3}})
      board.toggle(cell[0], cell[1]);
    tick(board);
    for (int tick = 2; tick <= 10; tick++) {
      // When
      Neighborhood.recomputedNeighborhoods = 0;
      tick(board);
      // Then: 바깥 블록 1 + glider가 걸친 하위 블록과 그 이웃만 (수정 전에는 64 + 1 = 65)
      assertTrue(
          Neighborhood.recomputedNeighborhoods <= 5,
          "tick " + tick + ": " + Neighborhood.recomputedNeighborhoods + "개 다시 계산");
    }
  }

  private static void tick(Cell board) {
    Cell d = Cell.DUMMY;
    if (board.figureNextState(d, d, d, d, d, d, d, d)) board.transition();
  }
}
