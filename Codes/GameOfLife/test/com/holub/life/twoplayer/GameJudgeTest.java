package com.holub.life.twoplayer;

import static com.holub.life.twoplayer.GameScript.finishPlacement;
import static com.holub.life.twoplayer.GameScript.pass;
import static com.holub.life.twoplayer.GameScript.place;
import static com.holub.life.twoplayer.GameScript.runSimulation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 종료 조건과 승패 판정 (docs/test-cases.md TP-E). StubBoard에 세대별 셀 수를 주입한다. 값은 rules.md 7장 예시의 실제 셀 수다.
 * 별도 표시가 없으면 N=2, M=2.
 */
class GameJudgeTest {
  private final StubBoard board = StubBoard.size64();

  /** 초기 배치를 끝내 라운드 1 시뮬레이션 직전으로 보낸다. */
  private TwoPlayerGame simulating(int generationsPerRound, int rounds) {
    TwoPlayerGame game = new TwoPlayerGame(board, new GameSettings(2, generationsPerRound, 2, rounds));
    finishPlacement(game, 2, board.columns());
    assertEquals(PhaseKind.SIMULATION, game.phase());
    return game;
  }

  private static int[] counts(int a, int b) {
    return new int[] {a, b};
  }

  /** 명세 예시 4: glider와 block이 부딪혀 세대 10에 동시 전멸 (세대 1~10). */
  private static int[][] example4Counts() {
    return new int[][] {
      counts(5, 4), counts(5, 4), counts(5, 4), counts(5, 4), counts(5, 4), counts(5, 4),
      counts(6, 5), counts(3, 3), counts(1, 2), counts(0, 0)
    };
  }

  /** 종료될 때까지 tick하고 진행한 세대 수를 돌려준다. */
  private static int tickUntilGameOver(TwoPlayerGame game, int limit) {
    int ticks = 0;
    while (game.phase() != PhaseKind.GAME_OVER && ticks < limit) {
      game.tick();
      ticks++;
    }
    return ticks;
  }

  @Test
  @DisplayName("TP-E-01 한쪽이 전멸하면 K세대를 기다리지 않고 바로 끝난다")
  void oneSideWipedOutEndsImmediately() {
    // Given: K=10, 세대 1에 A=0 (고립 셀 1개가 죽음), B=4 (block)
    TwoPlayerGame game = simulating(10, 1);
    board.scriptCounts(counts(0, 4));
    // When
    game.tick();
    // Then
    assertEquals(PhaseKind.GAME_OVER, game.phase());
    assertEquals(Optional.of(new GameResult(Outcome.B_WINS, EndReason.WIPE_OUT, 0, 4)), game.result());
    assertEquals(1, game.generationInRound());
  }

  @Test
  @DisplayName("TP-E-02 같은 세대에 동시 전멸하면 무승부 (명세 예시 4, 세대 10)")
  void simultaneousWipeOutIsDraw() {
    // Given
    TwoPlayerGame game = simulating(20, 1);
    board.scriptCounts(example4Counts());
    // When
    int ticks = tickUntilGameOver(game, 20);
    // Then
    assertEquals(10, ticks);
    assertEquals(Optional.of(new GameResult(Outcome.DRAW, EndReason.WIPE_OUT, 0, 0)), game.result());
  }

  @Test
  @DisplayName("TP-E-03 R라운드가 끝나면 셀이 많은 쪽이 이긴다 (명세 예시 3, B 4:3)")
  void moreCellsWinsAfterLastRound() {
    // Given: R=1, K=4, 매 세대 A=3 (blinker), B=4 (block)
    TwoPlayerGame game = simulating(4, 1);
    board.scriptCounts(counts(3, 4), counts(3, 4), counts(3, 4), counts(3, 4));
    // When
    runSimulation(game, 4);
    // Then
    assertEquals(PhaseKind.GAME_OVER, game.phase());
    assertEquals(
        Optional.of(new GameResult(Outcome.B_WINS, EndReason.ROUNDS_COMPLETED, 3, 4)), game.result());
  }

  @Test
  @DisplayName("TP-E-04 R라운드 종료 시 셀 수가 같으면 무승부")
  void equalCountIsDrawAfterLastRound() {
    // Given
    TwoPlayerGame game = simulating(3, 1);
    board.scriptCounts(counts(4, 4), counts(4, 4), counts(4, 4));
    // When
    runSimulation(game, 3);
    // Then
    assertEquals(
        Optional.of(new GameResult(Outcome.DRAW, EndReason.ROUNDS_COMPLETED, 4, 4)), game.result());
  }

  @Test
  @DisplayName("TP-E-05 배치·개입 단계에서는 전멸을 검사하지 않는다")
  void noWipeOutCheckDuringPlacement() {
    // Given / When: 초기 배치에서 A만 1개 (A=1, B=0)
    TwoPlayerGame game = new TwoPlayerGame(board, new GameSettings(2, 3, 2, 2));
    place(game, Player.A, 0, 0);
    // Then
    assertEquals(0, game.score(Player.B));
    assertEquals(PhaseKind.PLACEMENT, game.phase());
    assertEquals(Optional.empty(), game.result());

    // Given / When: 개입 중 A=0이 돼도 (입력을 처리해도) 판정하지 않는다
    place(game, Player.B, 0, 63);
    finishRestOfPlacement(game);
    runSimulation(game, 3);
    assertEquals(PhaseKind.INTERVENTION, game.phase());
    board.setCounts(0, 4);
    pass(game, Player.B);
    // Then
    assertEquals(PhaseKind.INTERVENTION, game.phase());
    assertEquals(Optional.empty(), game.result());
  }

  private static void finishRestOfPlacement(TwoPlayerGame game) {
    place(game, Player.A, 1, 0);
    place(game, Player.B, 1, 63);
  }

  @Test
  @DisplayName("TP-E-06 셀 수가 바뀌는 진동자는 K의 홀짝에 따라 판정 값이 달라진다 (beacon 6/8 vs block 4)")
  void oscillatorCountUsesFullKGenerations() {
    // K=1: 세대 1의 beacon은 6개
    TwoPlayerGame odd = simulating(1, 1);
    board.scriptCounts(counts(6, 4));
    runSimulation(odd, 1);
    assertEquals(
        Optional.of(new GameResult(Outcome.A_WINS, EndReason.ROUNDS_COMPLETED, 6, 4)), odd.result());

    // K=2: 세대 2의 beacon은 8개
    StubBoard second = StubBoard.size64().scriptCounts(counts(6, 4), counts(8, 4));
    TwoPlayerGame even = new TwoPlayerGame(second, new GameSettings(2, 2, 2, 1));
    finishPlacement(even, 2, second.columns());
    runSimulation(even, 2);
    assertEquals(
        Optional.of(new GameResult(Outcome.A_WINS, EndReason.ROUNDS_COMPLETED, 8, 4)), even.result());
  }

  @Test
  @DisplayName("TP-E-07 점수는 현재 보드에 살아 있는 자기 셀 수다 (명세 예시 4 세대 7: 6:5)")
  void scoreIsCurrentLiveCellCount() {
    // Given
    TwoPlayerGame game = simulating(20, 1);
    board.scriptCounts(example4Counts());
    // When
    runSimulation(game, 7);
    // Then
    assertEquals(6, game.score(Player.A));
    assertEquals(5, game.score(Player.B));
    assertEquals(PhaseKind.SIMULATION, game.phase());
  }

  @Test
  @DisplayName("TP-E-08 종료 뒤에는 선택·확정·패스·Tick 입력이 모두 무시된다")
  void inputsIgnoredAfterGameOver() {
    // Given: TP-E-01처럼 세대 1에 종료
    TwoPlayerGame game = simulating(10, 1);
    board.scriptCounts(counts(0, 4));
    game.tick();
    Optional<GameResult> before = game.result();
    // When / Then
    assertEquals(Response.WRONG_PHASE, game.select(Player.B, 10, 10));
    assertEquals(Response.WRONG_PHASE, game.confirm(Player.B));
    assertEquals(Response.WRONG_PHASE, game.pass(Player.B));
    assertFalse(game.tick());
    assertEquals(1, board.generations);
    assertEquals(before, game.result());
  }

  @Test
  @DisplayName("TP-E-09 마지막 라운드가 아니어도 K번째 세대에 전멸하면 개입으로 넘어가지 않고 종료한다")
  void wipeOutAtKthGenerationEndsGameBeforeIntervention() {
    // Given: R=2, K=3
    TwoPlayerGame game = simulating(3, 2);
    board.scriptCounts(counts(2, 1), counts(2, 1), counts(2, 0));
    // When
    runSimulation(game, 3);
    // Then
    assertEquals(PhaseKind.GAME_OVER, game.phase());
    assertEquals(1, game.round());
    assertEquals(Optional.of(new GameResult(Outcome.A_WINS, EndReason.WIPE_OUT, 2, 0)), game.result());
  }

  @Test
  @DisplayName("TP-E-10 판정 규칙만 숫자로 확인한다 (게임 없이)")
  void judgeRulesOnNumbersOnly() {
    // 세대 직후 전멸 검사
    assertEquals(
        Optional.of(new GameResult(Outcome.B_WINS, EndReason.WIPE_OUT, 0, 3)), GameJudge.afterGeneration(0, 3));
    assertEquals(
        Optional.of(new GameResult(Outcome.A_WINS, EndReason.WIPE_OUT, 3, 0)), GameJudge.afterGeneration(3, 0));
    assertEquals(
        Optional.of(new GameResult(Outcome.DRAW, EndReason.WIPE_OUT, 0, 0)), GameJudge.afterGeneration(0, 0));
    assertEquals(Optional.empty(), GameJudge.afterGeneration(2, 3));
    // 마지막 라운드 종료
    assertEquals(
        new GameResult(Outcome.A_WINS, EndReason.ROUNDS_COMPLETED, 5, 3), GameJudge.afterLastRound(5, 3));
    assertEquals(
        new GameResult(Outcome.B_WINS, EndReason.ROUNDS_COMPLETED, 3, 5), GameJudge.afterLastRound(3, 5));
    assertEquals(
        new GameResult(Outcome.DRAW, EndReason.ROUNDS_COMPLETED, 4, 4), GameJudge.afterLastRound(4, 4));
  }
}
