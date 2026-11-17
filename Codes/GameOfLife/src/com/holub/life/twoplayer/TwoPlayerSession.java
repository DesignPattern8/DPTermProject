package com.holub.life.twoplayer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 2인용 UI 진입점의 모델 쪽 (Facade, docs/007-two-player-ui.md 결정 3). 실제 보드와 {@link TwoPlayerGame}을 만들고, UI
 * 입력을 "현재 플레이어의 입력"으로 바꿔 넘기며, 상태 표시줄 문구를 만든다. Swing에 의존하지 않는다.
 *
 * <p>클릭은 EDT, tick은 시계 스레드에서 온다. 공개 메서드를 모두 {@code synchronized}로 묶어 {@link TwoPlayerGame}에는 한 번에
 * 하나씩만 들어가게 한다. 리스너는 잠금 밖에서, 입력을 처리한 스레드에서 부른다(결정 2, Observer).
 */
public final class TwoPlayerSession {
  /** 그리기용 스냅숏. {@code cells[row][column]}은 그 칸의 소유자, DEAD면 null. 받은 쪽이 고쳐도 게임은 바뀌지 않는다. */
  public record Snapshot(Player[][] cells, Optional<Coordinate> tentative, Player currentPlayer) {}

  private final GameSettings settings;
  private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
  private NeighborhoodTwoPlayerBoard board;
  private TwoPlayerGame game;
  private String rejection = ""; // 마지막 입력의 거부 이유. 받아들인 입력이 오면 지운다.

  public TwoPlayerSession(GameSettings settings) {
    this.settings = settings;
    start();
  }

  /** 상태가 바뀔 때마다(입력을 처리했거나 세대를 진행했을 때) 불린다. */
  public void addListener(Runnable listener) {
    listeners.add(listener);
  }

  // ---- 입력 ----

  /** 현재 플레이어의 임시 배치(또는 이동). 시뮬레이션·종료 단계에서는 {@link Response#WRONG_PHASE}. */
  public Response click(int row, int column) {
    Response response;
    synchronized (this) {
      Player who = game.currentPlayer();
      response = who == null ? Response.WRONG_PHASE : game.select(who, row, column);
      remember(response);
    }
    notifyListeners();
    return response;
  }

  /** 현재 플레이어의 임시 배치를 확정한다. */
  public Response confirm() {
    Response response;
    synchronized (this) {
      Player who = game.currentPlayer();
      response = who == null ? Response.WRONG_PHASE : game.confirm(who);
      remember(response);
    }
    notifyListeners();
    return response;
  }

  /** 현재 플레이어가 패스한다(개입 단계만). */
  public Response pass() {
    Response response;
    synchronized (this) {
      Player who = game.currentPlayer();
      response = who == null ? Response.WRONG_PHASE : game.pass(who);
      remember(response);
    }
    notifyListeners();
    return response;
  }

  /**
   * 시뮬레이션 단계에서 1세대 진행하고 true. 다른 단계에서는 아무것도 하지 않고 false이며, 리스너도 부르지 않는다(빠른 속도에서 문구가
   * 깜빡이지 않도록).
   */
  public boolean tick() {
    boolean advanced;
    synchronized (this) {
      advanced = game.tick();
      if (advanced) rejection = "";
    }
    if (advanced) notifyListeners();
    return advanced;
  }

  /** 빈 보드로 새 게임을 시작한다. */
  public void newGame() {
    synchronized (this) {
      start();
    }
    notifyListeners();
  }

  // ---- 조회 ----

  /** 상태 표시줄 한 줄 (docs/test-cases.md 5장의 조각을 포함한다). */
  public synchronized String status() {
    String state = state();
    return rejection.isEmpty() ? state : "Rejected: " + rejection + " | " + state;
  }

  public synchronized Snapshot snapshot() {
    Player[][] cells = new Player[board.rows()][board.columns()];
    for (int row = 0; row < cells.length; row++)
      for (int column = 0; column < cells[row].length; column++)
        cells[row][column] = board.owner(row, column).orElse(null);
    return new Snapshot(cells, game.tentative(), game.currentPlayer());
  }

  public synchronized PhaseKind phase() {
    return game.phase();
  }

  public synchronized int generationInRound() {
    return game.generationInRound();
  }

  public synchronized int rows() {
    return board.rows();
  }

  public synchronized int columns() {
    return board.columns();
  }

  // ---- 내부 ----

  private void start() {
    board = new NeighborhoodTwoPlayerBoard();
    game = new TwoPlayerGame(board, settings);
    rejection = "";
  }

  private void remember(Response response) {
    rejection = response == Response.ACCEPTED ? "" : reason(response, game.phase());
  }

  private void notifyListeners() {
    for (Runnable listener : listeners) listener.run();
  }

  private String state() {
    String cells = "A:B = " + game.score(Player.A) + ":" + game.score(Player.B);
    return switch (game.phase()) {
      case PLACEMENT -> "Placement | " + turnAndLeft() + " | " + cells;
      case INTERVENTION -> "Round " + game.round() + " Intervention | " + turnAndLeft() + " | " + cells;
      case SIMULATION ->
          "Round "
              + game.round()
              + " Simulation "
              + game.generationInRound()
              + "/"
              + settings.generationsPerRound()
              + " | "
              + cells
              + " | run with Go";
      case GAME_OVER -> gameOver() + " | " + cells;
    };
  }

  private String turnAndLeft() {
    String selected =
        game.tentative()
            .map(at -> " | Selected (" + at.row() + "," + at.column() + ")")
            .orElse("");
    return "Turn: "
        + game.currentPlayer()
        + " | Left A:"
        + game.remaining(Player.A)
        + " B:"
        + game.remaining(Player.B)
        + selected;
  }

  private String gameOver() {
    GameResult result = game.result().orElseThrow();
    String outcome =
        switch (result.outcome()) {
          case A_WINS -> "A wins";
          case B_WINS -> "B wins";
          case DRAW -> "Draw";
        };
    String why = result.reason() == EndReason.WIPE_OUT ? "wipe out" : "rounds completed";
    return "Game over: " + outcome + " (" + why + ")";
  }

  private static String reason(Response response, PhaseKind phase) {
    return switch (response) {
      case ACCEPTED -> "";
      case WRONG_PHASE ->
          phase == PhaseKind.SIMULATION ? "no placing during simulation" : "the game is over";
      case NOT_YOUR_TURN -> "not your turn";
      case OUT_OF_BOARD -> "outside the board";
      case CELL_NOT_EMPTY -> "the cell is not empty";
      case OPPONENT_HALF -> "opponent's half";
      case NO_TENTATIVE_PLACEMENT -> "select a cell first";
      case PASS_NOT_ALLOWED -> "no pass during initial placement";
    };
  }
}
