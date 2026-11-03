package com.holub.life.twoplayer;

/**
 * 개입: 각자 M개, 보드 어디든, 한 차례에 1개를 두거나 패스. 두 사람이 연달아 패스하면 끝난다. 개수가 0이거나 둘 빈칸이 없으면 자동
 * 패스 (rules.md 4장 3).
 */
final class InterventionPhase extends PlacingPhase {
  private final int round;
  private int consecutivePasses = 0;

  /** 선공은 배치 단계마다 바뀐다: 초기 배치 A, 라운드 1 개입 B, 라운드 2 개입 A, ... */
  InterventionPhase(TwoPlayerGame game, int round) {
    super(game, round % 2 == 1 ? Player.B : Player.A, game.settings().interventions());
    this.round = round;
  }

  /** 단계에 들어온 직후 호출한다. 선공이 둘 수 없으면 자동 패스한다. */
  void begin() {
    skipPlayersWhoCannotPlace();
  }

  @Override
  public PhaseKind kind() {
    return PhaseKind.INTERVENTION;
  }

  @Override
  protected boolean allowsColumn(Player who, int column) {
    return true;
  }

  @Override
  protected void afterConfirm() {
    consecutivePasses = 0;
    nextTurn();
  }

  @Override
  public Response pass(Player who) {
    if (who != current) return Response.NOT_YOUR_TURN;
    clearTentative();
    if (++consecutivePasses >= 2) {
      game.endIntervention(round);
      return Response.ACCEPTED;
    }
    nextTurn();
    return Response.ACCEPTED;
  }

  private void nextTurn() {
    current = current.opponent();
    skipPlayersWhoCannotPlace();
  }

  /** 차례가 온 플레이어가 둘 수 없으면 자동 패스. 연속 패스가 2가 되면 단계를 끝낸다. */
  private void skipPlayersWhoCannotPlace() {
    while (!canPlace(current)) {
      if (++consecutivePasses >= 2) {
        game.endIntervention(round);
        return;
      }
      current = current.opponent();
    }
  }

  private boolean canPlace(Player player) {
    return remaining(player) > 0 && game.board().hasEmptyCell();
  }
}
