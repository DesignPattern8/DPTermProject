package com.holub.life.twoplayer;

import static com.holub.life.twoplayer.GameScript.defaultTestSettings;
import static com.holub.life.twoplayer.GameScript.finishPlacement;
import static com.holub.life.twoplayer.GameScript.pass;
import static com.holub.life.twoplayer.GameScript.passBoth;
import static com.holub.life.twoplayer.GameScript.place;
import static com.holub.life.twoplayer.GameScript.runSimulation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 2인용 배치 규칙 (docs/test-cases.md TP-P). 64×64 StubBoard, A 진영 0~31열, B 진영 32~63열. N=2, K=3, M=2, R=2. */
class PlacementRuleTest {
  private final StubBoard board = StubBoard.size64();
  private final TwoPlayerGame game = new TwoPlayerGame(board, defaultTestSettings());

  private void toRound1Intervention() {
    finishPlacement(game, 2, 64);
    runSimulation(game, 3);
    assertEquals(PhaseKind.INTERVENTION, game.phase());
  }

  @Test
  @DisplayName("TP-P-01 자기 차례에 자기 진영 빈칸을 클릭하면 임시 배치된다")
  void clickPlacesTentativeCell() {
    // When
    Response r = game.select(Player.A, 10, 10);
    // Then
    assertEquals(Response.ACCEPTED, r);
    assertEquals(Optional.of(new Point(10, 10)), game.tentative());
    assertEquals(Player.A, game.currentPlayer());
    assertEquals(0, board.placeCalls); // 임시 배치는 보드에 놓지 않는다
  }

  @Test
  @DisplayName("TP-P-02 확정 전에 다른 빈칸을 클릭하면 임시 배치가 옮겨진다")
  void clickAnotherCellMovesTentative() {
    // Given
    game.select(Player.A, 10, 10);
    // When
    Response r = game.select(Player.A, 11, 11);
    // Then
    assertEquals(Response.ACCEPTED, r);
    assertEquals(Optional.of(new Point(11, 11)), game.tentative());
    assertTrue(board.isEmpty(10, 10));
    assertEquals(0, board.placeCalls);
  }

  @Test
  @DisplayName("TP-P-03 확정하면 셀이 고정되고 차례가 넘어가며 개수가 줄어든다")
  void confirmFixesCellAndPassesTurn() {
    // Given
    game.select(Player.A, 10, 10);
    // When
    Response r = game.confirm(Player.A);
    // Then
    assertEquals(Response.ACCEPTED, r);
    assertEquals(Player.A, board.owner(10, 10));
    assertEquals(Player.B, game.currentPlayer());
    assertEquals(1, game.remaining(Player.A));
    assertEquals(Optional.empty(), game.tentative());
  }

  @Test
  @DisplayName("TP-P-04 확정한 셀은 취소할 수 없다")
  void confirmedCellCannotBeUndone() {
    // Given
    place(game, Player.A, 10, 10);
    place(game, Player.B, 10, 63);
    // When: 다시 A 차례에 자기가 확정한 칸을 클릭
    Response r = game.select(Player.A, 10, 10);
    // Then
    assertEquals(Response.CELL_NOT_EMPTY, r);
    assertEquals(Player.A, board.owner(10, 10));
  }

  @Test
  @DisplayName("TP-P-05 자기·상대의 살아 있는 칸에는 둘 수 없다")
  void cannotPlaceOnLiveCell() {
    // Given
    board.preset(5, 5, Player.A).preset(5, 6, Player.B);
    // When / Then
    assertEquals(Response.CELL_NOT_EMPTY, game.select(Player.A, 5, 5));
    assertEquals(Response.CELL_NOT_EMPTY, game.select(Player.A, 5, 6));
    assertEquals(Optional.empty(), game.tentative());
    assertEquals(2, board.liveCount());
  }

  @Test
  @DisplayName("TP-P-06 초기 배치에서는 상대 진영에 둘 수 없다")
  void placementRejectsOpponentHalf() {
    assertEquals(Response.OPPONENT_HALF, game.select(Player.A, 10, 40));
    assertEquals(Optional.empty(), game.tentative());
  }

  @Test
  @DisplayName("TP-P-07 진영 경계: A는 31열 허용·32열 거부, B는 32열 허용·31열 거부")
  void halfBoundaryColumns() {
    // A
    assertEquals(Response.OPPONENT_HALF, game.select(Player.A, 10, 32));
    assertEquals(Response.ACCEPTED, game.select(Player.A, 10, 31));
    game.confirm(Player.A);
    // B
    assertEquals(Response.OPPONENT_HALF, game.select(Player.B, 11, 31));
    assertEquals(Response.ACCEPTED, game.select(Player.B, 11, 32));
  }

  @Test
  @DisplayName("TP-P-08 개입 단계에서는 상대 진영에도 둘 수 있다")
  void interventionAllowsOpponentHalf() {
    // Given: 라운드 1 개입은 B 선공
    toRound1Intervention();
    pass(game, Player.B);
    // When
    place(game, Player.A, 10, 40);
    // Then
    assertEquals(Player.A, board.owner(10, 40));
  }

  @Test
  @DisplayName("TP-P-09 차례가 아닌 플레이어는 둘 수 없다")
  void rejectsPlayerOutOfTurn() {
    assertEquals(Response.NOT_YOUR_TURN, game.select(Player.B, 10, 40));
    assertEquals(Response.NOT_YOUR_TURN, game.confirm(Player.B));
    assertEquals(Optional.empty(), game.tentative());
  }

  @Test
  @DisplayName("TP-P-10 남은 개수가 0인 플레이어는 둘 수 없다 (자동 패스로 차례가 오지 않음)")
  void rejectsWhenNoPlacementsLeft() {
    // Given: A가 개입 개수 M(=2)을 모두 씀
    toRound1Intervention();
    pass(game, Player.B);
    place(game, Player.A, 40, 10);
    pass(game, Player.B);
    place(game, Player.A, 41, 10);
    // When
    Response r = game.select(Player.A, 42, 10);
    // Then
    assertEquals(Response.NOT_YOUR_TURN, r);
    assertEquals(Player.B, game.currentPlayer());
  }

  @Test
  @DisplayName("TP-P-11 시뮬레이션·종료 단계에서는 배치·확정·패스 입력을 받지 않는다")
  void rejectsPlacementOutsidePlacementPhases() {
    // Given: 시뮬레이션
    finishPlacement(game, 2, 64);
    int placedBefore = board.placeCalls;
    // When / Then
    assertEquals(Response.WRONG_PHASE, game.select(Player.A, 20, 20));
    assertEquals(Response.WRONG_PHASE, game.confirm(Player.A));
    assertEquals(Response.WRONG_PHASE, game.pass(Player.A));
    // Given: 종료
    runSimulation(game, 3);
    passBoth(game);
    runSimulation(game, 3);
    assertEquals(PhaseKind.GAME_OVER, game.phase());
    // When / Then
    assertEquals(Response.WRONG_PHASE, game.select(Player.A, 20, 20));
    assertEquals(Response.WRONG_PHASE, game.confirm(Player.A));
    assertEquals(Response.WRONG_PHASE, game.pass(Player.B));
    assertEquals(placedBefore, board.placeCalls);
  }

  @Test
  @DisplayName("TP-P-12 보드 밖 좌표는 예외 없이 거부한다")
  void rejectsOutOfBoardCoordinate() {
    assertEquals(Response.OUT_OF_BOARD, game.select(Player.A, -1, 0));
    assertEquals(Response.OUT_OF_BOARD, game.select(Player.A, 0, 64));
    assertEquals(Response.OUT_OF_BOARD, game.select(Player.A, 64, 0));
    assertEquals(Optional.empty(), game.tentative());
  }

  @Test
  @DisplayName("TP-P-13 임시 배치 없이 확정할 수 없다")
  void confirmWithoutTentativeIsRejected() {
    assertEquals(Response.NO_TENTATIVE_PLACEMENT, game.confirm(Player.A));
    assertEquals(Player.A, game.currentPlayer());
    assertEquals(2, game.remaining(Player.A));
  }

  @Test
  @DisplayName("TP-P-14 초기 배치에서는 패스할 수 없다")
  void passRejectedDuringPlacement() {
    assertEquals(Response.PASS_NOT_ALLOWED, game.pass(Player.A));
    assertEquals(Player.A, game.currentPlayer());
  }

  @Test
  @DisplayName("TP-P-15 초기 배치의 마지막 수로 경계 건너편 상대 셀 옆에 두는 것은 허용된다")
  void lastPlacementNextToBorderIsAllowed() {
    // Given
    place(game, Player.A, 10, 31);
    place(game, Player.B, 0, 63);
    place(game, Player.A, 1, 0);
    // When: B의 N(=2)번째 수
    Response r = game.select(Player.B, 10, 32);
    // Then
    assertEquals(Response.ACCEPTED, r);
    assertEquals(Response.ACCEPTED, game.confirm(Player.B));
    assertEquals(PhaseKind.SIMULATION, game.phase());
    assertNull(game.currentPlayer());
  }
}
