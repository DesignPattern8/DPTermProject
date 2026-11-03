package com.holub.life.twoplayer;

import java.util.Optional;

/** 승패 판정 규칙 (rules.md 6장, docs/005-cell-count-and-judge.md). 구현 전 골격. */
final class GameJudge {
  private GameJudge() {}

  static Optional<GameResult> afterGeneration(int scoreA, int scoreB) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  static GameResult afterLastRound(int scoreA, int scoreB) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
