package com.holub.life.twoplayer;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

/**
 * TwoPlayerBoard 대역. 셀 상태 표현(#5) 전이라 게임 진행 테스트에서 실제 보드 대신 쓴다. 확정 배치된 칸을 기억해 isEmpty에 반영하고,
 * advanceGeneration() 호출 수를 센다. 세대 진행은 보드를 바꾸지 않는다(= 정물 보드).
 */
final class StubBoard implements TwoPlayerBoard {
  private final int rows;
  private final int columns;
  private final Map<Point, Player> cells = new HashMap<>();
  int generations = 0;
  int placeCalls = 0;
  /** true면 모든 칸이 차 있는 것처럼 동작한다 (자동 패스 테스트용). */
  boolean full = false;

  StubBoard(int rows, int columns) {
    this.rows = rows;
    this.columns = columns;
  }

  static StubBoard size64() {
    return new StubBoard(64, 64);
  }

  /** 테스트 준비용: 게임을 거치지 않고 칸에 셀을 둔다. */
  StubBoard preset(int row, int column, Player owner) {
    cells.put(new Point(column, row), owner);
    return this;
  }

  Player owner(int row, int column) {
    return cells.get(new Point(column, row));
  }

  int liveCount() {
    return cells.size();
  }

  @Override
  public int rows() {
    return rows;
  }

  @Override
  public int columns() {
    return columns;
  }

  @Override
  public boolean isEmpty(int row, int column) {
    return !full && !cells.containsKey(new Point(column, row));
  }

  @Override
  public boolean hasEmptyCell() {
    return !full && cells.size() < rows * columns;
  }

  @Override
  public void place(int row, int column, Player owner) {
    if (!isEmpty(row, column)) throw new IllegalStateException("place() on a non-empty cell");
    placeCalls++;
    cells.put(new Point(column, row), owner);
  }

  @Override
  public void advanceGeneration() {
    generations++;
  }
}
