package com.holub.life.ui;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.model.Cell;
import com.holub.ui.MenuSite;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Existing Life window configuration. */
public final class LifeFrame extends JFrame {
  public LifeFrame(Cell board, GameController controller, Clock clock) {
    this(board, controller, clock, List.of());
  }

  /**
   * {@code otherModes}가 있으면 Mode 메뉴(Classic + 각 모드)를 달고, 모드 화면을 {@link ModeSwitch}로 바꾼다
   * (twoplayer/docs/007-two-player-ui.md). 비어 있으면 Mode 메뉴 없이 클래식 화면만 보인다.
   */
  public LifeFrame(Cell board, GameController controller, Clock clock, List<GameMode> otherModes) {
    super("The Game of Life. " + "(c)2003 Allen I. Holub <http://www.holub.com>");
    MenuSite.establish(this);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());
    PresetPlacement presets = new PresetPlacement(controller, new SwingUserDialogs());
    BoardPanel panel = new BoardPanel(board, controller, presets);
    GameMenu.installMenus(panel, controller, clock, presets);
    ClassicMode classic = new ClassicMode(panel, controller);
    ModeSwitch modes =
        new ModeSwitch(
            clock,
            classic,
            enabled -> {
              // A mode switch cancels a waiting preset; presets are Classic-only (decision 010).
              presets.cancel();
              GameMenu.setFileCommandsEnabled(panel, enabled);
              GameMenu.setPresetsEnabled(panel, controller, enabled);
            });
    if (!otherModes.isEmpty()) {
      MenuSite.addLine(modes, "Mode", classic.menuName(), e -> modes.select(classic));
      for (GameMode mode : otherModes) {
        modes.add(mode);
        MenuSite.addLine(modes, "Mode", mode.menuName(), e -> modes.select(mode));
      }
    }
    getContentPane().add(modes.view(), BorderLayout.CENTER);
    controller.addGameListener(
        new GameResultAnnouncer(controller, new SwingUserDialogs(this), SwingUtilities::invokeLater));
    pack();
    setVisible(true);
  }
}
