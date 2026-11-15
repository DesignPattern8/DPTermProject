package com.holub.life.twoplayer;

import java.util.HashMap;
import java.util.Map;

/** 실제 보드({@link NeighborhoodTwoPlayerBoard}) 테스트 도우미. 좌표는 {row, column} 배열로 받는다. */
final class RealBoardScript {
  private RealBoardScript() {}

  /** owner의 셀을 차례로 놓는다. */
  static void place(NeighborhoodTwoPlayerBoard board, Player owner, int[]... cells) {
    for (int[] cell : cells) {
      board.place(cell[0], cell[1], owner);
    }
  }

  static void advance(NeighborhoodTwoPlayerBoard board, int generations) {
    for (int i = 0; i < generations; i++) {
      board.advanceGeneration();
    }
  }

  /** 지금 살아 있는 모든 칸과 소유자. */
  static Map<Coordinate, Player> liveCells(NeighborhoodTwoPlayerBoard board) {
    Map<Coordinate, Player> live = new HashMap<>();
    for (int r = 0; r < board.rows(); r++) {
      for (int c = 0; c < board.columns(); c++) {
        final int row = r;
        final int column = c;
        board.owner(r, c).ifPresent(owner -> live.put(new Coordinate(row, column), owner));
      }
    }
    return live;
  }

  /** 기대값용: owner의 칸 목록을 map에 더한다. 체이닝으로 A·B를 함께 만든다. */
  static Map<Coordinate, Player> cells(Player owner, int[]... cells) {
    return with(new HashMap<>(), owner, cells);
  }

  static Map<Coordinate, Player> with(
      Map<Coordinate, Player> map, Player owner, int[]... cells) {
    for (int[] cell : cells) {
      map.put(new Coordinate(cell[0], cell[1]), owner);
    }
    return map;
  }

  static int[] at(int row, int column) {
    return new int[] {row, column};
  }

  /** {A 셀 수, B 셀 수}. */
  static int[] counts(NeighborhoodTwoPlayerBoard board) {
    return new int[] {board.count(Player.A), board.count(Player.B)};
  }
}
