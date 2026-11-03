package com.holub.life.twoplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 테스트에서 게임을 원하는 단계까지 진행시키는 도우미. 각 입력이 받아들여졌는지도 확인한다. */
final class GameScript {
  private GameScript() {}

  /** TP-F 기본 설정: N=2, K=3, M=2, R=2 */
  static GameSettings defaultTestSettings() {
    return new GameSettings(2, 3, 2, 2);
  }

  static void place(TwoPlayerGame game, Player who, int row, int column) {
    assertEquals(Response.ACCEPTED, game.select(who, row, column), who + " select (" + row + "," + column + ")");
    assertEquals(Response.ACCEPTED, game.confirm(who), who + " confirm (" + row + "," + column + ")");
  }

  static void pass(TwoPlayerGame game, Player who) {
    assertEquals(Response.ACCEPTED, game.pass(who), who + " pass");
  }

  /** 초기 배치를 끝낸다. A는 i행 0열, B는 i행 마지막 열에 차례로 둔다. */
  static void finishPlacement(TwoPlayerGame game, int placements, int columns) {
    for (int i = 0; i < placements; i++) {
      place(game, Player.A, i, 0);
      place(game, Player.B, i, columns - 1);
    }
  }

  static void runSimulation(TwoPlayerGame game, int generations) {
    for (int g = 0; g < generations; g++) assertTrue(game.tick(), "tick " + (g + 1));
  }

  /** 현재 개입 단계를 양쪽 연속 패스로 끝낸다. */
  static void passBoth(TwoPlayerGame game) {
    Player first = game.currentPlayer();
    pass(game, first);
    pass(game, first.opponent());
  }
}
