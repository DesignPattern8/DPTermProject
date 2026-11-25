package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.application.CancerGame;
import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.application.GameListener;
import com.holub.life.application.TickSource;
import com.holub.life.model.CompositeBoard;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;
import com.holub.life.ui.ClassicMode;
import com.holub.life.ui.ModeSwitch;
import com.holub.life.ui.SinglePlayerMode;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.JPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** UI-R21~R24: 원본 클래식 모드와 1인용(암세포) 모드 분리 (ui/docs/test-cases.md, ui/docs/010). */
class ClassicModeTest {
  private static final class IdleTicks implements TickSource {
    @Override
    public void start(int intervalMillis, Runnable onTick) {}

    @Override
    public void stop() {}
  }

  /** 확률 검사가 항상 통과하고, 후보는 항상 첫 번째를 고른다. */
  private static final class AlwaysCancer extends Random {
    @Override
    public double nextDouble() {
      return 0.0;
    }

    @Override
    public int nextInt(int bound) {
      return 0;
    }
  }

  private final Clock clock = new Clock(new IdleTicks());

  private static Neighborhood newBoard() {
    return new Neighborhood(8, new Neighborhood(8, new Resident()));
  }

  private GameController classicController(Neighborhood board) {
    return new GameController(new CompositeBoard(board), board, clock);
  }

  private GameController singlePlayerController(Neighborhood board, Random random) {
    return new GameController(
        new CompositeBoard(board), board, clock, new CancerGame(board, random));
  }

  private static Set<Point> liveCells(Neighborhood board) {
    Set<Point> live = new HashSet<>();
    var memento = board.createMemento();
    for (int r = 0; r < 64; r++)
      for (int c = 0; c < 64; c++) if (memento.isAlive(new Point(c, r))) live.add(new Point(c, r));
    return live;
  }

  private static void blinker(Neighborhood board) {
    board.toggle(10, 9);
    board.toggle(10, 10);
    board.toggle(10, 11);
  }

  private static boolean horizontalBlinker(Neighborhood board) {
    return liveCells(board).equals(Set.of(new Point(9, 10), new Point(10, 10), new Point(11, 10)));
  }

  @Test
  @DisplayName("UI-R21 클래식 모드는 원본 B3/S23대로 진행하고 암세포를 만들지 않는다")
  void classicModeFollowsOriginalRulesWithoutCancer() {
    Neighborhood board = newBoard();
    // R-펜토미노: 오래 변하는 패턴이라 300세대 동안 많은 경우를 지난다.
    int[][] rPentomino = {{30, 31}, {30, 32}, {31, 30}, {31, 31}, {32, 31}};
    for (int[] rc : rPentomino) board.toggle(rc[0], rc[1]);
    GameController controller = classicController(board);
    List<GameListener.Change> changes = new ArrayList<>();
    controller.addGameListener(changes::add);
    ClassicMode classic = new ClassicMode(new JPanel(), controller);
    ReferenceLife reference = new ReferenceLife(64, liveCells(board));

    for (int generation = 1; generation <= 300; generation++) {
      classic.tick();
      reference.tick();
      assertEquals(reference.liveCells(), liveCells(board), "세대 " + generation);
      assertEquals(0, board.cancerCount(), "세대 " + generation);
    }
    assertFalse(changes.contains(GameListener.Change.GAME_OVER));
    assertNull(controller.gameResultMessage());
  }

  @Test
  @DisplayName("UI-R22 메뉴 이름은 Classic과 Single Player로 나뉘고 둘 다 파일 명령을 쓴다")
  void modesHaveDistinctMenuNames() {
    ClassicMode classic = new ClassicMode(new JPanel(), classicController(newBoard()));
    Neighborhood board = newBoard();
    SinglePlayerMode single =
        new SinglePlayerMode(new JPanel(), singlePlayerController(board, new Random(1)));

    assertEquals("Classic", classic.menuName());
    assertEquals("Single Player", single.menuName());
    assertTrue(classic.supportsFileCommands());
    assertTrue(single.supportsFileCommands());
  }

  @Test
  @DisplayName("UI-R23 1인용 모드의 tick은 암세포 규칙을 적용한다")
  void singlePlayerTickAppliesCancerRules() {
    Neighborhood board = newBoard();
    int[][] block = {{20, 20}, {20, 21}, {21, 20}, {21, 21}};
    for (int[] rc : block) board.toggle(rc[0], rc[1]);
    SinglePlayerMode single =
        new SinglePlayerMode(new JPanel(), singlePlayerController(board, new AlwaysCancer()));

    single.tick();

    assertTrue(board.cancerCount() > 0);
  }

  @Test
  @DisplayName("UI-R24 클래식과 1인용은 각자 보드를 갖고, 선택한 모드의 보드만 진행한다")
  void onlySelectedModeAdvances() {
    Neighborhood classicBoard = newBoard();
    Neighborhood singleBoard = newBoard();
    blinker(classicBoard);
    blinker(singleBoard);
    ClassicMode classic = new ClassicMode(new JPanel(), classicController(classicBoard));
    // 새 암세포가 생기지 않도록 확률 검사가 항상 실패하는 대역
    Random noCancer =
        new Random() {
          @Override
          public double nextDouble() {
            return 0.99;
          }
        };
    SinglePlayerMode single =
        new SinglePlayerMode(new JPanel(), singlePlayerController(singleBoard, noCancer));
    ModeSwitch modes = new ModeSwitch(clock, classic, enabled -> {});
    modes.add(single);
    modes.select(single);

    clock.tick();

    assertTrue(horizontalBlinker(classicBoard), "클래식 보드는 그대로");
    assertEquals(
        Set.of(new Point(10, 9), new Point(10, 10), new Point(10, 11)),
        liveCells(singleBoard),
        "1인용 blinker만 세로로");
  }
}
