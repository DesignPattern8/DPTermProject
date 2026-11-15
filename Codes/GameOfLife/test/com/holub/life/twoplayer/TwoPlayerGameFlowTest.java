package com.holub.life.twoplayer;

import static com.holub.life.twoplayer.GameScript.defaultTestSettings;
import static com.holub.life.twoplayer.GameScript.finishPlacement;
import static com.holub.life.twoplayer.GameScript.pass;
import static com.holub.life.twoplayer.GameScript.passBoth;
import static com.holub.life.twoplayer.GameScript.place;
import static com.holub.life.twoplayer.GameScript.runSimulation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 2인용 게임 진행 (docs/test-cases.md TP-F). 보드는 StubBoard, 기본 설정 N=2, K=3, M=2, R=2. */
class TwoPlayerGameFlowTest {
  private final StubBoard board = StubBoard.size64();

  private TwoPlayerGame newGame(GameSettings settings) {
    return new TwoPlayerGame(board, settings);
  }

  private TwoPlayerGame newGame() {
    return newGame(defaultTestSettings());
  }

  /** 초기 배치와 라운드 1 시뮬레이션을 끝내 라운드 1 개입 단계로 보낸다. */
  private TwoPlayerGame atRound1Intervention(GameSettings settings) {
    TwoPlayerGame game = newGame(settings);
    finishPlacement(game, settings.placements(), board.columns());
    runSimulation(game, settings.generationsPerRound());
    assertEquals(PhaseKind.INTERVENTION, game.phase());
    return game;
  }

  @Test
  @DisplayName("TP-F-01 새 게임은 초기 배치 단계에서 A 차례로 시작한다")
  void startsInPlacementWithA() {
    // When
    TwoPlayerGame game = newGame();
    // Then
    assertEquals(PhaseKind.PLACEMENT, game.phase());
    assertEquals(Player.A, game.currentPlayer());
    assertEquals(2, game.remaining(Player.A));
    assertEquals(2, game.remaining(Player.B));
    assertEquals(0, game.round());
  }

  @Test
  @DisplayName("TP-F-02 초기 배치는 1개씩 번갈아 둔다")
  void placementAlternatesOneByOne() {
    // Given
    TwoPlayerGame game = newGame();
    // When / Then
    place(game, Player.A, 0, 0);
    assertEquals(Player.B, game.currentPlayer());
    place(game, Player.B, 0, 63);
    assertEquals(Player.A, game.currentPlayer());
    place(game, Player.A, 1, 0);
    assertEquals(Player.B, game.currentPlayer());
  }

  @Test
  @DisplayName("TP-F-03 양쪽이 N개씩 두면 시뮬레이션 단계로 넘어간다")
  void placementEndsAfterNEach() {
    // Given
    TwoPlayerGame game = newGame();
    // When
    finishPlacement(game, 2, 64);
    // Then
    assertEquals(PhaseKind.SIMULATION, game.phase());
    assertEquals(1, game.round());
    assertEquals(0, game.generationInRound());
  }

  @Test
  @DisplayName("TP-F-04 시뮬레이션은 K세대 뒤 개입 단계로 넘어간다")
  void simulationEndsAfterKGenerations() {
    // Given
    TwoPlayerGame game = newGame();
    finishPlacement(game, 2, 64);
    // When / Then
    runSimulation(game, 2);
    assertEquals(PhaseKind.SIMULATION, game.phase());
    assertEquals(2, game.generationInRound());
    runSimulation(game, 1);
    assertEquals(PhaseKind.INTERVENTION, game.phase());
    assertEquals(3, board.generations);
  }

  @Test
  @DisplayName("TP-F-05 개입 단계를 시작하면 각자 M개를 받는다")
  void interventionGrantsMEach() {
    // When
    TwoPlayerGame game = atRound1Intervention(defaultTestSettings());
    // Then: 초기 배치를 다 써서 0이던 개수가 M(=2)이 된다
    assertEquals(2, game.remaining(Player.A));
    assertEquals(2, game.remaining(Player.B));
  }

  @Test
  @DisplayName("TP-F-06 두 사람이 연달아 패스하면 개입이 끝난다")
  void twoConsecutivePassesEndIntervention() {
    // Given
    TwoPlayerGame game = atRound1Intervention(defaultTestSettings());
    // When
    pass(game, Player.B);
    pass(game, Player.A);
    // Then
    assertEquals(PhaseKind.SIMULATION, game.phase());
    assertEquals(2, game.round());
    assertEquals(0, game.generationInRound());
  }

  @Test
  @DisplayName("TP-F-07 패스 사이에 배치가 끼면 '연달아'가 아니다")
  void passPlacePassDoesNotEnd() {
    // Given
    TwoPlayerGame game = atRound1Intervention(defaultTestSettings());
    // When
    pass(game, Player.B);
    place(game, Player.A, 40, 10);
    pass(game, Player.B);
    // Then
    assertEquals(PhaseKind.INTERVENTION, game.phase());
    assertEquals(Player.A, game.currentPlayer());
  }

  @Test
  @DisplayName("TP-F-08 선공과 후공은 초기 배치부터 이어서 번갈아 간다 (라운드 1 개입 선공 B, 라운드 2 개입 선공 A)")
  void interventionFirstPlayerAlternates() {
    // Given
    GameSettings settings = new GameSettings(2, 3, 2, 3);
    TwoPlayerGame game = atRound1Intervention(settings);
    // Then
    assertEquals(Player.B, game.currentPlayer());
    // When
    passBoth(game);
    runSimulation(game, 3);
    // Then
    assertEquals(PhaseKind.INTERVENTION, game.phase());
    assertEquals(2, game.round());
    assertEquals(Player.A, game.currentPlayer());
  }

  @Test
  @DisplayName("TP-F-09 개수가 0인 플레이어는 자동으로 패스한다")
  void autoPassWhenNoPlacementsLeft() {
    // Given: A가 개입 개수 M(=2)을 모두 쓴다
    TwoPlayerGame game = atRound1Intervention(defaultTestSettings());
    pass(game, Player.B);
    place(game, Player.A, 40, 10);
    pass(game, Player.B);
    place(game, Player.A, 41, 10);
    assertEquals(0, game.remaining(Player.A));
    assertEquals(Player.B, game.currentPlayer());
    // When: B가 패스하면 A 차례는 자동 패스 → 연속 2패스
    pass(game, Player.B);
    // Then
    assertEquals(PhaseKind.SIMULATION, game.phase());
    assertEquals(2, game.round());
  }

  @Test
  @DisplayName("TP-F-10 둘 빈칸이 없으면 양쪽이 자동으로 패스해 개입이 바로 끝난다")
  void autoPassWhenNoEmptyCell() {
    // Given
    TwoPlayerGame game = newGame();
    finishPlacement(game, 2, 64);
    runSimulation(game, 2);
    board.full = true;
    // When: K번째 세대로 개입에 들어가는 순간
    runSimulation(game, 1);
    // Then
    assertEquals(PhaseKind.SIMULATION, game.phase());
    assertEquals(2, game.round());
    assertEquals(0, game.generationInRound());
  }

  @Test
  @DisplayName("TP-F-11 쓰지 않은 개수는 다음 라운드로 넘어가지 않는다")
  void unusedPlacementsAreDiscarded() {
    // Given: R=3, 라운드 1 개입에서 B는 1개만 쓰고 끝냄
    GameSettings settings = new GameSettings(2, 3, 2, 3);
    TwoPlayerGame game = atRound1Intervention(settings);
    place(game, Player.B, 40, 50);
    pass(game, Player.A);
    pass(game, Player.B);
    // When
    runSimulation(game, 3);
    // Then
    assertEquals(PhaseKind.INTERVENTION, game.phase());
    assertEquals(2, game.remaining(Player.B));
    assertEquals(2, game.remaining(Player.A));
  }

  @Test
  @DisplayName("TP-F-12 R번째 라운드의 시뮬레이션 뒤에는 개입 없이 종료한다")
  void gameEndsAfterLastSimulation() {
    // Given: R=2
    TwoPlayerGame game = atRound1Intervention(defaultTestSettings());
    passBoth(game);
    runSimulation(game, 2);
    assertEquals(PhaseKind.SIMULATION, game.phase());
    // When
    runSimulation(game, 1);
    // Then
    assertEquals(PhaseKind.GAME_OVER, game.phase());
    assertEquals(null, game.currentPlayer());
  }

  @Test
  @DisplayName("TP-F-13 보드가 정체돼도 K세대를 모두 진행한다 (대역 보드는 세대 진행으로 바뀌지 않음)")
  void stableBoardStillRunsAllKGenerations() {
    // Given
    TwoPlayerGame game = newGame();
    finishPlacement(game, 2, 64);
    // When
    runSimulation(game, 2);
    // Then
    assertEquals(PhaseKind.SIMULATION, game.phase());
    runSimulation(game, 1);
    assertEquals(PhaseKind.INTERVENTION, game.phase());
    assertEquals(3, board.generations);
  }

  @Test
  @DisplayName("TP-F-14 시뮬레이션 단계의 수동 Tick은 1세대로 K에 포함된다")
  void manualTickCountsTowardK() {
    // Given: 모델에서 수동 Tick(메뉴)과 자동 진행(타이머)은 둘 다 tick() 한 번이다. 메뉴 연결은 #7.
    TwoPlayerGame game = newGame();
    finishPlacement(game, 2, 64);
    // When
    game.tick(); // 수동 Tick 1회
    runSimulation(game, 2); // 자동 진행
    // Then
    assertEquals(PhaseKind.INTERVENTION, game.phase());
  }

  @Test
  @DisplayName("TP-F-15 시뮬레이션이 아닌 단계의 Tick은 무시된다")
  void tickIgnoredOutsideSimulation() {
    // Given
    TwoPlayerGame game = newGame();
    // When / Then: 초기 배치
    assertFalse(game.tick());
    assertEquals(0, board.generations);
    assertEquals(PhaseKind.PLACEMENT, game.phase());
    // When / Then: 개입
    finishPlacement(game, 2, 64);
    runSimulation(game, 3);
    assertFalse(game.tick());
    assertEquals(3, board.generations);
    assertEquals(PhaseKind.INTERVENTION, game.phase());
  }

  @Test
  @DisplayName("TP-F-16 설정을 주입하지 않으면 기본값 N=20, K=20, M=3, R=5를 쓴다")
  void defaultSettings() {
    // When
    GameSettings settings = GameSettings.defaults();
    // Then
    assertEquals(20, settings.placements());
    assertEquals(20, settings.generationsPerRound());
    assertEquals(3, settings.interventions());
    assertEquals(5, settings.rounds());
    assertEquals(20, newGame(settings).remaining(Player.A));
  }

  @Test
  @DisplayName("TP-F-17 잘못된 설정값(N·K·R ≤ 0, M < 0)은 거부한다")
  void rejectsInvalidSettings() {
    assertThrows(IllegalArgumentException.class, () -> new GameSettings(0, 3, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> new GameSettings(2, 0, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> new GameSettings(2, 3, -1, 2));
    assertThrows(IllegalArgumentException.class, () -> new GameSettings(2, 3, 2, 0));
    new GameSettings(2, 3, 0, 2); // M = 0은 허용
  }
}
