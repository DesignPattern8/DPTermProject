package com.holub.life.twoplayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TwoPlayerBoard 대역. 셀 상태 표현(#5) 전이라 게임 진행 테스트에서 실제 보드 대신 쓴다. 확정 배치된 칸을 기억해 isEmpty에 반영하고,
 * advanceGeneration() 호출 수를 센다. 세대 진행은 보드를 바꾸지 않는다(= 정물 보드).
 *
 * <p>셀 수(count)는 기본적으로 놓인 칸 수를 센다. 승패 판정 테스트는 scriptCounts()로 세대마다의 셀 수를 주입한다(세대 진행 1번마다
 * 다음 값).
 */
final class StubBoard implements TwoPlayerBoard {
  private final int rows;
  private final int columns;
  private final Map<Coordinate, Player> cells = new HashMap<>();
  int generations = 0;
  int placeCalls = 0;
  /** true면 모든 칸이 차 있는 것처럼 동작한다 (자동 패스 테스트용). */
  boolean full = false;

  private final Deque<int[]> scriptedCounts = new ArrayDeque<>();
  private int[] currentCounts = null; // null이면 놓인 칸 수를 센다

  /** 세대 진행 1번마다 차례로 쓸 셀 수 {A, B}. */
  StubBoard scriptCounts(int[]... perGeneration) {
    scriptedCounts.addAll(List.of(perGeneration));
    return this;
  }

  /** 지금 셀 수를 바로 정한다. */
  StubBoard setCounts(int countA, int countB) {
    currentCounts = new int[] {countA, countB};
    return this;
  }

  StubBoard(int rows, int columns) {
    this.rows = rows;
    this.columns = columns;
  }

  static StubBoard size64() {
    return new StubBoard(64, 64);
  }

  /** 테스트 준비용: 게임을 거치지 않고 칸에 셀을 둔다. */
  StubBoard preset(int row, int column, Player owner) {
    cells.put(new Coordinate(row, column), owner);
    return this;
  }

  Player owner(int row, int column) {
    return cells.get(new Coordinate(row, column));
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
    return !full && !cells.containsKey(new Coordinate(row, column));
  }

  @Override
  public boolean hasEmptyCell() {
    return !full && cells.size() < rows * columns;
  }

  @Override
  public void place(int row, int column, Player owner) {
    if (!isEmpty(row, column)) throw new IllegalStateException("place() on a non-empty cell");
    placeCalls++;
    cells.put(new Coordinate(row, column), owner);
  }

  @Override
  public void advanceGeneration() {
    generations++;
    if (!scriptedCounts.isEmpty()) currentCounts = scriptedCounts.poll();
  }

  @Override
  public int count(Player owner) {
    if (currentCounts != null) return currentCounts[owner == Player.A ? 0 : 1];
    return (int) cells.values().stream().filter(owner::equals).count();
  }
}
