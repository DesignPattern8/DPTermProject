package com.holub.life.twoplayer;

import java.util.Optional;

/**
 * 게임 단계 (State 패턴, docs/003-game-phase.md). TwoPlayerGame은 모든 입력을 현재 단계에 위임하고, 단계가 다음 단계로 바꾼다.
 */
interface Phase {
  PhaseKind kind();

  Response select(Player who, int row, int column);

  Response confirm(Player who);

  Response pass(Player who);

  /** 시뮬레이션 단계에서만 1세대 진행하고 true. */
  boolean tick();

  /** 배치를 받는 단계의 현재 차례. 그 밖의 단계는 null. */
  Player currentPlayer();

  int remaining(Player player);

  Optional<Coordinate> tentative();
}
