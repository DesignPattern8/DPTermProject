package com.holub.life.twoplayer;

/**
 * 게임 진행이 보드에 묻는 것만 모은 인터페이스. 셀 상태 표현(#5)을 정하기 전이라 게임 진행을 셀 상태와 분리한다. 계약은
 * docs/interfaces.md.
 */
public interface TwoPlayerBoard {
  int rows();

  int columns();

  /** 그 칸이 DEAD이면 true. 범위 검사는 호출하는 쪽이 먼저 한다. */
  boolean isEmpty(int row, int column);

  /** isEmpty가 true인 칸이 하나라도 있으면 true. */
  boolean hasEmptyCell();

  /** 그 칸을 owner의 산 셀로 만든다. 사전조건: isEmpty(row, column). */
  void place(int row, int column, Player owner);

  /** rules.md 3장 규칙으로 1세대 진행한다. */
  void advanceGeneration();
}
