package com.holub.life.ui;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.model.Cell;
import com.holub.ui.MenuSite;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Existing Life window configuration. */
public final class LifeFrame extends JFrame {
  /** 클래식 화면만 (Mode 메뉴 없음). */
  public LifeFrame(Cell board, GameController controller, Clock clock) {
    this(board, controller, null, null, clock, List.of());
  }

  /**
   * 클래식(원본 Life), 1인용(암세포 게임), {@code otherModes}(2인용 등)를 Mode 메뉴로 오간다 (ui/docs/010,
   * twoplayer/docs/007). 시작 화면은 클래식이다. {@code singleBoard}가 null이면 1인용을 붙이지 않고, 1인용·{@code
   * otherModes}가 모두 없으면 Mode 메뉴 없이 클래식 화면만 보인다.
   *
   * <p>Grid 메뉴(Clear·Load·Store)는 하나만 달고, 누른 순간 선택된 모드의 컨트롤러로 보낸다.
   */
  public LifeFrame(
      Cell classicBoard,
      GameController classicController,
      Cell singleBoard,
      GameController singleController,
      Clock clock,
      List<GameMode> otherModes) {
    super("The Game of Life. " + "(c)2003 Allen I. Holub <http://www.holub.com>");
    MenuSite.establish(this);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());

    BoardPanel classicPanel = new BoardPanel(classicBoard, classicController);
    ClassicMode classic = new ClassicMode(classicPanel, classicController);
    Map<GameMode, GameController> controllers = new IdentityHashMap<>();
    controllers.put(classic, classicController);
    List<GameMode> selectable = new ArrayList<>();
    selectable.add(classic);
    if (singleBoard != null) {
      SinglePlayerMode single =
          new SinglePlayerMode(new BoardPanel(singleBoard, singleController), singleController);
      controllers.put(single, singleController);
      selectable.add(single);
    }
    selectable.addAll(otherModes);

    ModeSwitch[] modes = new ModeSwitch[1];
    GameMenu.installMenus(
        classicPanel,
        () -> controllers.getOrDefault(modes[0].current(), classicController),
        clock);
    modes[0] =
        new ModeSwitch(
            clock, classic, enabled -> GameMenu.setFileCommandsEnabled(classicPanel, enabled));
    for (GameMode mode : selectable) {
      if (mode != classic) modes[0].add(mode);
      if (selectable.size() > 1)
        MenuSite.addLine(modes[0], "Mode", mode.menuName(), e -> modes[0].select(mode));
    }
    getContentPane().add(modes[0].view(), BorderLayout.CENTER);

    SwingUserDialogs dialogs = new SwingUserDialogs(this);
    for (GameController controller : new LinkedHashSet<>(controllers.values()))
      controller.addGameListener(
          new GameResultAnnouncer(controller, dialogs, SwingUtilities::invokeLater));
    pack();
    setVisible(true);
  }
}
