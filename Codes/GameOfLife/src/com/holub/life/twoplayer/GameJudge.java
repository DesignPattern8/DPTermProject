package com.holub.life.twoplayer;

import java.util.Optional;

/**
 * 승패 판정 규칙 (rules.md 6장, docs/005-cell-count-and-judge.md). 셀 수 두 개만 받는 순수 함수라 게임·보드 없이 확인할 수 있다.
 * 언제 판정하는지는 SimulationPhase가 정한다.
 */
final class GameJudge {
  private GameJudge() {}

  /** 시뮬레이션 중 매 세대 직후. 한쪽이라도 0이면 결과, 아니면 empty. */
  static Optional<GameResult> afterGeneration(int scoreA, int scoreB) {
    if (scoreA > 0 && scoreB > 0) return Optional.empty();
    Outcome outcome =
        scoreA == 0 && scoreB == 0 ? Outcome.DRAW : scoreA == 0 ? Outcome.B_WINS : Outcome.A_WINS;
    return Optional.of(new GameResult(outcome, EndReason.WIPE_OUT, scoreA, scoreB));
  }

  /** R번째 라운드의 시뮬레이션이 전멸 없이 끝났을 때. 셀 수가 많은 쪽 승, 같으면 무승부. */
  static GameResult afterLastRound(int scoreA, int scoreB) {
    Outcome outcome =
        scoreA > scoreB ? Outcome.A_WINS : scoreB > scoreA ? Outcome.B_WINS : Outcome.DRAW;
    return new GameResult(outcome, EndReason.ROUNDS_COMPLETED, scoreA, scoreB);
  }
}
