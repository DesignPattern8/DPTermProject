package com.holub.life.twoplayer;

/** 게임 단계의 종류 (rules.md 4장). UI와 테스트가 현재 단계를 확인할 때 쓴다. */
public enum PhaseKind {
  PLACEMENT,
  SIMULATION,
  INTERVENTION,
  GAME_OVER
}
