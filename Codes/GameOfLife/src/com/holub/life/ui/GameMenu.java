package com.holub.life.ui;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.application.Speed;
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
        e -> controller.clear());
    MenuSite.addLine(panel, "Grid", "Load", e -> load(controller));
    MenuSite.addLine(panel, "Grid", "Store", e -> store(controller));
    MenuSite.addLine(panel, "Grid", "Exit", e -> System.exit(0));

    ActionListener modifier = e -> go(clock, ((JMenuItem) e.getSource()).getName());
    MenuSite.addLine(clock, "Go", "Halt", modifier);
    MenuSite.addLine(clock, "Go", "Tick (Single Step)", modifier);
    MenuSite.addLine(clock, "Go", "Agonizing", modifier);
    MenuSite.addLine(clock, "Go", "Slow", modifier);
    MenuSite.addLine(clock, "Go", "Medium", modifier);
    MenuSite.addLine(clock, "Go", "Fast", modifier);
    clock.addClockListener(() -> onTick(controller));
  }

  /** Map a Go menu item to the clock request. Like the original, any other item stops the clock. */
  static void go(Clock clock, String name) {
    switch (name) {
      case "Tick (Single Step)" -> clock.tick();
      case "Agonizing" -> clock.start(Speed.AGONIZING);
      case "Slow" -> clock.start(Speed.SLOW);
      case "Medium" -> clock.start(Speed.MEDIUM);
      case "Fast" -> clock.start(Speed.FAST);
      default -> clock.stop(); // "Halt"
    }
  }

  /** Advance a generation unless a menu is open. Redrawing is done by the panel's GameListener. */
  static void onTick(GameController controller) {
    if (!menuIsActive()) controller.tick();
  }

  private static boolean menuIsActive() {
    MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
    return path != null && path.length > 0;
  }

  private static void load(GameController controller) {
    try {
      controller.load(FileDialogs.userSelected(".", ".life", "Life File", "Load"));
    } catch (IOException e) {
      JOptionPane.showMessageDialog(
          null, "Read Failed!", "The Game of Life", JOptionPane.ERROR_MESSAGE);
    }
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
