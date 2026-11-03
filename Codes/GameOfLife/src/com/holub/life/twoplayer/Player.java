package com.holub.life.twoplayer;

/** 2인용 모드의 두 플레이어. A는 보드 왼쪽 절반, B는 오른쪽 절반이 진영이다 (rules.md 2장). */
public enum Player {
  A,
  B;

  public Player opponent() {
    return this == A ? B : A;
  }
}
