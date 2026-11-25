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
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Existing Life window configuration. */
public final class LifeFrame extends JFrame {
  /** 클래식 화면만 (Mode 메뉴·Preset 메뉴 없음). */
  public LifeFrame(Cell board, GameController controller, Clock clock) {
    this(board, controller, null, null, clock, List.of());
  }

  /**
   * 클래식(원본 Life), 1인용(암세포 게임), {@code otherModes}(2인용 등)를 Mode 메뉴로 오간다 (ui/docs/012,
   * twoplayer/docs/007). 시작 화면은 클래식이다. {@code singleBoard}가 null이면 1인용을 붙이지 않고, 1인용·{@code
   * otherModes}가 모두 없으면 Mode 메뉴 없이 클래식 화면만 보인다.
   *
   * <p>Grid 메뉴(Clear·Load·Store)는 한 벌만 달고, 누른 순간 선택된 모드의 컨트롤러로 보낸다. Preset 메뉴(ui/docs/010)는
   * 1인용에서만 켜진다. 클래식은 원본에 없는 기능을 쓰지 않는다.
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

    PresetPlacement presets = null;
    SinglePlayerMode single = null;
    if (singleBoard != null) {
      presets = new PresetPlacement(singleController, new SwingUserDialogs());
      single =
          new SinglePlayerMode(
              new BoardPanel(singleBoard, singleController, presets), singleController);
      controllers.put(single, singleController);
      selectable.add(single);
    }
    selectable.addAll(otherModes);

    ModeSwitch[] modes = new ModeSwitch[1];
    Supplier<GameController> active =
        () -> controllers.getOrDefault(modes[0].current(), classicController);
    if (presets != null)
      GameMenu.installMenus(classicPanel, active, clock, singleController, presets);
    else GameMenu.installMenus(classicPanel, active, clock, classicController, null);

    PresetPlacement placement = presets;
    GameMode presetMode = single;
    modes[0] =
        new ModeSwitch(
            clock,
            classic,
            enabled -> {
              boolean presetsOn = modes[0] != null && modes[0].current() == presetMode;
              if (placement != null) {
                // A mode switch cancels a waiting preset (decision 010).
                placement.cancel();
                GameMenu.setPresetsEnabled(classicPanel, singleController, presetsOn);
              }
              GameMenu.setFileCommandsEnabled(classicPanel, enabled);
            });
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
