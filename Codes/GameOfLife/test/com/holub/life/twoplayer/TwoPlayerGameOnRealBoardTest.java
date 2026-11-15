package com.holub.life.twoplayer;

import static com.holub.life.twoplayer.RealBoardScript.at;
import static com.holub.life.twoplayer.RealBoardScript.liveCells;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 실제 보드로 게임 진행 다시 확인 (docs/test-cases.md 1-3장 TP-I). StubBoard 대신
 * {@link NeighborhoodTwoPlayerBoard}를 넣고 TP-F-13, TP-E-02, TP-E-03을 다시 돌린다.
 */
class TwoPlayerGameOnRealBoardTest {
  private final NeighborhoodTwoPlayerBoard board = new NeighborhoodTwoPlayerBoard();

  /** 초기 배치: A, B가 각자의 목록에서 앞에서부터 하나씩 번갈아 select → confirm 한다. */
  private static void placeAlternately(TwoPlayerGame game, int[][] cellsA, int[][] cellsB) {
    assertEquals(cellsA.length, cellsB.length);
    for (int i = 0; i < cellsA.length; i++) {
      GameScript.place(game, Player.A, cellsA[i][0], cellsA[i][1]);
      GameScript.place(game, Player.B, cellsB[i][0], cellsB[i][1]);
    }
  }

  private static int[] scores(TwoPlayerGame game) {
    return new int[] {game.score(Player.A), game.score(Player.B)};
  }

  @Test
  @DisplayName("TP-I-01 실제 정물 보드에서도 K세대를 모두 진행한 뒤 개입으로 간다 (TP-F-13)")
  void stableRealBoardStillRunsAllKGenerations() {
    // Given: N=4, K=3, M=2, R=2, block 2개
    TwoPlayerGame game = new TwoPlayerGame(board, new GameSettings(4, 3, 2, 2));
    placeAlternately(
        game,
        new int[][] {at(10, 10), at(10, 11), at(11, 10), at(11, 11)},
        new int[][] {at(10, 50), at(10, 51), at(11, 50), at(11, 51)});
    Map<Coordinate, Player> generation0 = liveCells(board);
    // When
    GameScript.runSimulation(game, 2);
    // Then
    assertEquals(PhaseKind.SIMULATION, game.phase());
    assertEquals(2, game.generationInRound());
    // When
    GameScript.runSimulation(game, 1);
    // Then
    assertEquals(PhaseKind.INTERVENTION, game.phase());
    assertEquals(1, game.round());
    assertArrayEquals(new int[] {4, 4}, scores(game));
    assertEquals(generation0, liveCells(board));
  }

  @Test
  @DisplayName("TP-I-02 실제 보드에서 같은 세대 동시 전멸은 무승부로 끝난다 (TP-E-02, 명세 예시 4)")
  void simultaneousWipeOutIsDrawOnRealBoard() {
    // Given: N=5, K=20, M=2, R=1. 예시 4를 오른쪽으로 27열 옮김 + B 고립 셀 (40,60)
    TwoPlayerGame game = new TwoPlayerGame(board, new GameSettings(5, 20, 2, 1));
    placeAlternately(
        game,
        new int[][] {at(0, 28), at(1, 29), at(2, 27), at(2, 28), at(2, 29)},
        new int[][] {at(5, 32), at(5, 33), at(6, 32), at(6, 33), at(40, 60)});
    int[][] expected = {
      {5, 5}, {5, 4}, {5, 4}, {5, 4}, {5, 4}, {5, 4}, {5, 4}, {6, 5}, {3, 3}, {1, 2}, {0, 0}
    };
    assertArrayEquals(expected[0], scores(game), "generation 0");
    for (int generation = 1; generation <= 10; generation++) {
      // When
      assertTrue(game.tick(), "tick " + generation);
      // Then
      assertArrayEquals(expected[generation], scores(game), "generation " + generation);
    }
    assertEquals(PhaseKind.GAME_OVER, game.phase());
    assertEquals(
        Optional.of(new GameResult(Outcome.DRAW, EndReason.WIPE_OUT, 0, 0)), game.result());
    assertFalse(game.tick(), "tick 11");
  }

  @Test
  @DisplayName("TP-I-03 실제 보드에서 R라운드 종료 시 셀이 많은 쪽이 이긴다 (TP-E-03, 명세 예시 3)")
  void moreCellsWinsAfterLastRoundOnRealBoard() {
    // Given: N=4, K=4, M=2, R=1. A blinker + 고립 셀 (40,10), B block을 40열로 옮김
    TwoPlayerGame game = new TwoPlayerGame(board, new GameSettings(4, 4, 2, 1));
    placeAlternately(
        game,
        new int[][] {at(2, 1), at(2, 2), at(2, 3), at(40, 10)},
        new int[][] {at(2, 40), at(2, 41), at(3, 40), at(3, 41)});
    assertArrayEquals(new int[] {4, 4}, scores(game), "generation 0");
    for (int generation = 1; generation <= 4; generation++) {
      // When
      assertTrue(game.tick(), "tick " + generation);
      // Then
      assertArrayEquals(new int[] {3, 4}, scores(game), "generation " + generation);
    }
    assertEquals(PhaseKind.GAME_OVER, game.phase());
    assertEquals(
        Optional.of(new GameResult(Outcome.B_WINS, EndReason.ROUNDS_COMPLETED, 3, 4)),
        game.result());
  }
}
