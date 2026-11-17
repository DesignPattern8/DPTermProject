package com.holub.life.twoplayer.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.application.Speed;
import com.holub.life.application.TickSource;
import com.holub.life.model.CompositeBoard;
import com.holub.life.model.CellPosition;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;
import com.holub.life.twoplayer.Coordinate;
import com.holub.life.twoplayer.GameSettings;
import com.holub.life.twoplayer.PhaseKind;
import com.holub.life.twoplayer.Player;
import com.holub.life.twoplayer.TwoPlayerSession;
import com.holub.life.ui.ClassicMode;
import com.holub.life.ui.GameMode;
import com.holub.life.ui.ModeSwitch;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TP-U: 2인용 UI 통합 (docs/test-cases.md 5장, docs/007-two-player-ui.md). Swing 창 없이 headless로 돈다. */
class TwoPlayerUiTest {
  private static final GameSettings DEFAULTS = GameSettings.defaults();
  /** N=1, K=3, M=1, R=2: A·B 한 개씩 두면 바로 시뮬레이션. 셀 1개는 1세대 뒤 죽으므로 tick 없이만 쓴다. */
  private static final GameSettings ONE_EACH = new GameSettings(1, 3, 1, 2);
  /** N=4, K=3, M=1, R=2: A·B가 블록(2×2, 정물)을 하나씩 두면 시뮬레이션. tick해도 셀 수가 그대로다. */
  private static final GameSettings BLOCKS = new GameSettings(4, 3, 1, 2);

  /** 시계 대역: 실제로 tick을 만들지 않고 start·stop만 기록한다. */
  private static final class RecordingTicks implements TickSource {
    boolean running;
    int stops;

    @Override
    public void start(int intervalMillis, Runnable onTick) {
      running = true;
    }

    @Override
    public void stop() {
      running = false;
      stops++;
    }
  }

  /** tick이 불린 횟수만 세는 대역 모드. */
  private static class StubMode implements GameMode {
    final JComponent view = new JPanel();
    final AtomicInteger ticks = new AtomicInteger();
    private final String name;

    StubMode(String name) {
      this.name = name;
    }

    @Override
    public String menuName() {
      return name;
    }

    @Override
    public JComponent view() {
      return view;
    }

    @Override
    public void tick() {
      ticks.incrementAndGet();
    }

    @Override
    public boolean supportsFileCommands() {
      return true;
    }
  }

  private final RecordingTicks ticks = new RecordingTicks();
  private final Clock clock = new Clock(ticks);
  private final Neighborhood classicBoard =
      new Neighborhood(8, new Neighborhood(8, new Resident()));
  private final GameController controller =
      new GameController(new CompositeBoard(classicBoard), classicBoard, clock);
  private final List<Boolean> fileCommands = new ArrayList<>();

  TwoPlayerUiTest() {
    classicBoard.toggle(10, 9);
    classicBoard.toggle(10, 10);
    classicBoard.toggle(10, 11);
  }

  @AfterEach
  void clearMenuSelection() {
    MenuSelectionManager.defaultManager().clearSelectedPath();
  }

  // ---- 도우미 ----

  private ClassicMode classic() {
    return new ClassicMode(new JPanel(), controller);
  }

  private static TwoPlayerMode twoPlayer(GameSettings settings) {
    TwoPlayerMode mode = new TwoPlayerMode(settings, Runnable::run);
    mode.panel().boardView().setSize(512, 512);
    return mode;
  }

  private ModeSwitch modes(GameMode first, GameMode... others) {
    ModeSwitch modes = new ModeSwitch(clock, first, fileCommands::add);
    for (GameMode other : others) modes.add(other);
    return modes;
  }

  private boolean classicAlive(int row, int column) {
    return classicBoard.createMemento().isAlive(new CellPosition(row, column));
  }

  private boolean classicBlinkerIsHorizontal() {
    return classicAlive(10, 9) && classicAlive(10, 10) && classicAlive(10, 11)
        && !classicAlive(9, 10) && !classicAlive(11, 10);
  }

  private boolean classicBlinkerIsVertical() {
    return classicAlive(9, 10) && classicAlive(10, 10) && classicAlive(11, 10)
        && !classicAlive(10, 9) && !classicAlive(10, 11);
  }

  /** 보드 영역의 (row, column) 칸 가운데를 왼쪽 버튼으로 누른다(칸 8px). */
  private static void clickCell(TwoPlayerMode mode, int row, int column) {
    press(mode.panel().boardView(), column * 8 + 4, row * 8 + 4);
  }

  private static void press(Component target, int x, int y) {
    target.dispatchEvent(
        new MouseEvent(
            target, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1));
  }

  /** A (10,10), B (10,40)을 확정한다. */
  private static void placeBoth(TwoPlayerMode mode) {
    clickCell(mode, 10, 10);
    mode.panel().confirmButton().doClick();
    clickCell(mode, 10, 40);
    mode.panel().confirmButton().doClick();
  }

  /** A 블록 (10,10)~(11,11), B 블록 (10,40)~(11,41)을 번갈아 확정한다(N=4). */
  private static void placeBlocks(TwoPlayerMode mode) {
    int[][] offsets = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
    for (int[] offset : offsets) {
      clickCell(mode, 10 + offset[0], 10 + offset[1]);
      mode.panel().confirmButton().doClick();
      clickCell(mode, 10 + offset[0], 40 + offset[1]);
      mode.panel().confirmButton().doClick();
    }
  }

  private static Optional<Player> owner(TwoPlayerSession session, int row, int column) {
    return Optional.ofNullable(session.snapshot().cells()[row][column]);
  }

  private static Component visibleCard(ModeSwitch modes) {
    Component shown = null;
    for (Component card : modes.view().getComponents()) {
      if (card.isVisible()) {
        assertNull(shown, "한 번에 카드 하나만 보여야 한다");
        shown = card;
      }
    }
    return shown;
  }

  private static void openMenu() {
    MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] {new JMenu("Go")});
  }

  // ---- TP-U-01 ~ 17 ----

  @Test
  @DisplayName("TP-U-01 모드를 바꾸면 화면과 클릭 동작이 바뀐다")
  void switchingModeChangesClickBehavior() {
    TwoPlayerMode two = twoPlayer(DEFAULTS);
    ModeSwitch modes = modes(classic(), two);

    modes.select(two);
    clickCell(two, 10, 10);

    assertSame(two.view(), visibleCard(modes));
    assertEquals(Optional.of(new Coordinate(10, 10)), two.session().snapshot().tentative());
    assertEquals(Optional.empty(), owner(two.session(), 10, 10), "임시 배치는 보드에 놓지 않는다");
    assertTrue(classicBlinkerIsHorizontal());
  }

  @Test
  @DisplayName("TP-U-02 상태 표시줄에 단계·차례·남은 개수·A:B 셀 수가 나온다")
  void statusBarShowsGameState() {
    TwoPlayerMode two = twoPlayer(DEFAULTS);

    clickCell(two, 10, 10);
    two.panel().confirmButton().doClick();

    String status = two.panel().statusLabel().getText();
    assertTrue(status.contains("Placement"), status);
    assertTrue(status.contains("Turn: B"), status);
    assertTrue(status.contains("Left A:19 B:20"), status);
    assertTrue(status.contains("A:B = 1:0"), status);
  }

  @Test
  @DisplayName("TP-U-03 모델 상태가 바뀌면 상태 표시줄이 갱신된다")
  void statusBarUpdatesOnModelChange() {
    TwoPlayerMode two = twoPlayer(DEFAULTS);
    AtomicInteger notified = new AtomicInteger();
    two.session().addListener(notified::incrementAndGet);
    String before = two.panel().statusLabel().getText();

    clickCell(two, 10, 10);

    assertEquals(1, notified.get());
    assertEquals(two.session().status(), two.panel().statusLabel().getText());
    assertNotEquals(before, two.panel().statusLabel().getText());
  }

  @Test
  @DisplayName("TP-U-04 Enter 키로 임시 배치를 확정한다")
  void enterConfirmsPlacement() {
    TwoPlayerMode two = twoPlayer(DEFAULTS);
    clickCell(two, 10, 10);

    TwoPlayerPanel panel = two.panel();
    Object key = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("ENTER"));
    panel.getActionMap().get(key).actionPerformed(null);

    TwoPlayerSession session = two.session();
    assertEquals(Optional.of(Player.A), owner(session, 10, 10));
    assertEquals(Player.B, session.snapshot().currentPlayer());
    assertTrue(session.status().contains("Left A:19"), session.status());
  }

  @Test
  @DisplayName("TP-U-05 A·B 셀이 다른 색으로 그려지고 진영 경계선이 보인다")
  void rendersOwnerColorsAndBorderLine() {
    TwoPlayerMode two = twoPlayer(DEFAULTS);
    placeBoth(two);
    BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);

    Graphics g = image.getGraphics();
    try {
      TwoPlayerRenderer.render(g, new Rectangle(0, 0, 512, 512), two.session().snapshot());
    } finally {
      g.dispose();
    }

    int a = image.getRGB(84, 84);
    int b = image.getRGB(324, 84);
    assertEquals(TwoPlayerRenderer.A_COLOR.getRGB(), a);
    assertEquals(TwoPlayerRenderer.B_COLOR.getRGB(), b);
    assertNotEquals(a, b);
    assertEquals(TwoPlayerRenderer.EMPTY_COLOR.getRGB(), image.getRGB(164, 164));
    assertEquals(TwoPlayerRenderer.BORDER_COLOR.getRGB(), image.getRGB(256, 300));
  }

  @Test
  @DisplayName("TP-U-06 2인용 모드에서는 Store(와 Clear·Load)를 쓸 수 없다")
  void storeDisabledInTwoPlayerMode() {
    TwoPlayerMode two = twoPlayer(DEFAULTS);
    ModeSwitch modes = modes(classic(), two);
    assertEquals(List.of(true), fileCommands);

    modes.select(two);

    assertFalse(two.supportsFileCommands());
    assertEquals(List.of(true, false), fileCommands);
  }

  @Test
  @DisplayName("TP-U-07 시뮬레이션 중 클릭은 무시되고 이유가 표시된다")
  void clickDuringSimulationShowsReason() {
    TwoPlayerMode two = twoPlayer(ONE_EACH);
    placeBoth(two);
    assertEquals(PhaseKind.SIMULATION, two.session().phase());

    clickCell(two, 20, 20);

    TwoPlayerSession session = two.session();
    assertEquals(Optional.empty(), session.snapshot().tentative());
    assertEquals(Optional.empty(), owner(session, 20, 20));
    assertTrue(two.panel().statusLabel().getText().contains("Rejected:"), session.status());
  }

  @Test
  @DisplayName("TP-U-08 2인용에서 클래식으로 돌아오면 기존 기능이 그대로 동작한다")
  void classicFeaturesWorkAfterSwitchingBack(@TempDir Path directory) throws Exception {
    ClassicMode classic = classic();
    TwoPlayerMode two = twoPlayer(DEFAULTS);
    ModeSwitch modes = modes(classic, two);
    modes.select(two);

    modes.select(classic);

    assertEquals(List.of(true, false, true), fileCommands, "파일 명령이 다시 켜진다");
    // Store → Load
    Path file = directory.resolve("blinker.life");
    controller.store(file.toFile());
    controller.clear();
    controller.load(file.toFile());
    assertTrue(classicBlinkerIsHorizontal(), "Store한 blinker를 Load로 되살린다");
    // Go: Tick
    clock.tick();
    assertTrue(classicBlinkerIsVertical(), "Tick 1번에 세대가 진행한다");
    // Go: 속도와 Halt가 쓰는 같은 시계 (메뉴 이름 → 시계 요청은 GameMenuTest#goCommandsKeepOriginalActions)
    clock.start(Speed.FAST);
    assertTrue(ticks.running);
    clock.stop();
    assertFalse(ticks.running);
    // 마우스 토글
    controller.toggle(0, 0);
    assertTrue(classicAlive(0, 0));
    // Clear
    controller.clear();
    for (int row = 0; row < 64; row++)
      for (int column = 0; column < 64; column++) assertFalse(classicAlive(row, column));
  }

  @Test
  @DisplayName("TP-U-09 패스 버튼으로 개입 차례를 넘긴다")
  void passButtonPassesTurn() {
    TwoPlayerMode two = twoPlayer(new GameSettings(4, 1, 1, 2));
    placeBlocks(two);
    two.tick();
    assertEquals(PhaseKind.INTERVENTION, two.session().phase());
    assertEquals(Player.B, two.session().snapshot().currentPlayer());

    two.panel().passButton().doClick();

    assertEquals(Player.A, two.session().snapshot().currentPlayer());
    assertTrue(two.panel().statusLabel().getText().contains("Turn: A"));
  }

  @Test
  @DisplayName("TP-U-10 2인용 모드의 Tick은 시뮬레이션 세대를 진행하고 K에 들어간다")
  void tickAdvancesTwoPlayerSimulation() {
    TwoPlayerMode two = twoPlayer(BLOCKS);
    ModeSwitch modes = modes(classic(), two);
    placeBlocks(two);
    modes.select(two);

    clock.tick();

    assertEquals(1, two.session().generationInRound());
    assertTrue(two.session().status().contains("Simulation 1/3"), two.session().status());
  }

  @Test
  @DisplayName("TP-U-11 2인용 모드일 때 클래식 보드는 진행하지 않는다")
  void classicBoardDoesNotAdvanceInTwoPlayerMode() {
    TwoPlayerMode two = twoPlayer(BLOCKS);
    ModeSwitch modes = modes(classic(), two);
    placeBlocks(two);
    modes.select(two);

    clock.tick();
    clock.tick();
    clock.tick();

    assertTrue(classicBlinkerIsHorizontal());
  }

  @Test
  @DisplayName("TP-U-12 배치 단계의 tick은 아무것도 바꾸지 않는다")
  void tickDuringPlacementIsIgnored() {
    TwoPlayerMode two = twoPlayer(DEFAULTS);
    ModeSwitch modes = modes(classic(), two);
    modes.select(two);
    AtomicInteger notified = new AtomicInteger();
    two.session().addListener(notified::incrementAndGet);
    String before = two.panel().statusLabel().getText();

    clock.tick();

    assertEquals(0, notified.get());
    assertEquals(before, two.panel().statusLabel().getText());
    assertEquals(PhaseKind.PLACEMENT, two.session().phase());
  }

  @Test
  @DisplayName("TP-U-13 모드 전환은 진행 중인 tick이 끝난 뒤에 된다 (#74 회피)")
  void switchWaitsForRunningTick() throws Exception {
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    StubMode slow =
        new StubMode("slow") {
          @Override
          public void tick() {
            entered.countDown();
            try {
              release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
        };
    StubMode other = new StubMode("other");
    ModeSwitch modes = modes(slow, other);
    Thread ticker = new Thread(clock::tick);
    ticker.start();
    assertTrue(entered.await(5, TimeUnit.SECONDS));

    Thread switcher = new Thread(() -> modes.select(other));
    switcher.start();
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (switcher.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline)
      Thread.onSpinWait();

    assertEquals(Thread.State.BLOCKED, switcher.getState(), "tick이 끝날 때까지 전환을 기다린다");
    assertSame(slow, modes.current());
    release.countDown();
    switcher.join(5000);
    ticker.join(5000);
    assertSame(other, modes.current());
  }

  @Test
  @DisplayName("TP-U-14 모드를 바꾸면 시계가 멈춘다")
  void switchingModeStopsClock() {
    TwoPlayerMode two = twoPlayer(DEFAULTS);
    ModeSwitch modes = modes(classic(), two);
    clock.start(Speed.FAST);
    int stopsBefore = ticks.stops;

    modes.select(two);

    assertEquals(stopsBefore + 1, ticks.stops);
    assertFalse(ticks.running);
  }

  @Test
  @DisplayName("TP-U-15 새 게임 버튼은 2인용 게임을 처음으로 되돌린다")
  void newGameButtonResetsGame() {
    TwoPlayerMode two = twoPlayer(DEFAULTS);
    clickCell(two, 10, 10);
    two.panel().confirmButton().doClick();
    clickCell(two, 10, 40);

    two.panel().newGameButton().doClick();

    TwoPlayerSession session = two.session();
    for (Player[] row : session.snapshot().cells())
      for (Player cell : row) assertNull(cell);
    assertEquals(Optional.empty(), session.snapshot().tentative());
    assertEquals(PhaseKind.PLACEMENT, session.phase());
    assertEquals(Player.A, session.snapshot().currentPlayer());
    assertTrue(session.status().contains("Left A:20 B:20"), session.status());
  }

  @Test
  @DisplayName("TP-U-16 메뉴가 열려 있으면 2인용 tick도 건너뛴다")
  void openMenuSuppressesTwoPlayerTick() {
    TwoPlayerMode two = twoPlayer(BLOCKS);
    ModeSwitch modes = modes(classic(), two);
    placeBlocks(two);
    modes.select(two);

    openMenu();
    clock.tick();
    assertEquals(0, two.session().generationInRound());

    MenuSelectionManager.defaultManager().clearSelectedPath();
    clock.tick();
    assertEquals(1, two.session().generationInRound());
  }

  @Test
  @DisplayName("TP-U-17 격자 밖 픽셀 클릭은 무시되고 이유가 표시된다")
  void clickOutsideGridShowsReason() {
    TwoPlayerMode two = twoPlayer(DEFAULTS);
    two.panel().boardView().setSize(520, 520);

    press(two.panel().boardView(), 515, 10);

    assertEquals(Optional.empty(), two.session().snapshot().tentative());
    assertTrue(
        two.panel().statusLabel().getText().contains("Rejected: outside the board"),
        two.session().status());
  }
}
