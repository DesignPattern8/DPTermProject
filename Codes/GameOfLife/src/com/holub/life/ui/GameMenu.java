package com.holub.life.ui;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.ui.MenuSite;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;

/** The original Grid and Go actions, including the menu-open tick filter. */
public final class GameMenu {
  private GameMenu() {}

  public static void install(BoardPanel panel, GameController controller, Clock clock) {
    MenuSite.addLine(
        panel,
        "Grid",
        "Clear",
        e -> {
          controller.clear();
          panel.repaint();
        });
    MenuSite.addLine(panel, "Grid", "Load", e -> load(panel, controller));
    MenuSite.addLine(panel, "Grid", "Store", e -> store(controller));
    MenuSite.addLine(panel, "Grid", "Exit", e -> System.exit(0));

    ActionListener modifier = e -> go(clock, ((JMenuItem) e.getSource()).getName());
    MenuSite.addLine(clock, "Go", "Halt", modifier);
    MenuSite.addLine(clock, "Go", "Tick (Single Step)", modifier);
    MenuSite.addLine(clock, "Go", "Agonizing", modifier);
    MenuSite.addLine(clock, "Go", "Slow", modifier);
    MenuSite.addLine(clock, "Go", "Medium", modifier);
    MenuSite.addLine(clock, "Go", "Fast", modifier);
    Runnable refresh = panel::refreshNow;
    clock.addClockListener(() -> onTick(controller, refresh));
  }

  static void go(Clock clock, String name) {
    char toDo = name.charAt(0);
    if (toDo == 'T') clock.tick();
    else
      clock.startTicking(
          toDo == 'A' ? 500 : toDo == 'S' ? 150 : toDo == 'M' ? 70 : toDo == 'F' ? 30 : 0);
  }

  static void onTick(GameController controller, Runnable refresh) {
    if (!menuIsActive() && controller.tick()) refresh.run();
  }

  private static boolean menuIsActive() {
    MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
    return path != null && path.length > 0;
  }

  private static void load(BoardPanel panel, GameController controller) {
    try {
      controller.load(FileDialogs.userSelected(".", ".life", "Life File", "Load"));
    } catch (IOException e) {
      JOptionPane.showMessageDialog(
          null, "Read Failed!", "The Game of Life", JOptionPane.ERROR_MESSAGE);
    }
    panel.repaint();
  }

  private static void store(GameController controller) {
    try {
      controller.store(FileDialogs.userSelected(".", ".life", "Life File", "Write"));
    } catch (IOException e) {
      JOptionPane.showMessageDialog(
          null, "Write Failed!", "The Game of Life", JOptionPane.ERROR_MESSAGE);
    }
  }
}
