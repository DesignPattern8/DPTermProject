package com.holub.life.ui;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.application.Speed;
import com.holub.ui.MenuSite;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** The original Grid and Go menus as one table of entries, plus the menu-open tick filter. */
public final class GameMenu {
  static final String GRID = "Grid";
  static final String GO = "Go";

  private GameMenu() {}

  /** One menu item: the menu it belongs to, its label and what it does. */
  public record Entry(String menu, String label, ActionListener action) {}

  /**
   * The menu items in their original order: Grid (Clear, Load, Store, Exit), then Go (Halt, Tick,
   * Agonizing, Slow, Medium, Fast). Building the table needs no window.
   */
  public static List<Entry> entries(
      GameController controller, Clock clock, UserDialogs dialogs, Runnable exit) {
    List<Entry> entries =
        new ArrayList<>(
            List.of(
                new Entry(GRID, "Clear", e -> controller.clear()),
                new Entry(GRID, "Load", e -> load(controller, dialogs)),
                new Entry(GRID, "Store", e -> store(controller, dialogs)),
                new Entry(GRID, "Exit", e -> exit.run())));
    entries.addAll(goEntries(clock));
    return List.copyOf(entries);
  }

  private static List<Entry> goEntries(Clock clock) {
    return List.of(
        new Entry(GO, "Halt", e -> clock.stop()),
        new Entry(GO, "Tick (Single Step)", e -> clock.tick()),
        new Entry(GO, "Agonizing", e -> clock.start(Speed.AGONIZING)),
        new Entry(GO, "Slow", e -> clock.start(Speed.SLOW)),
        new Entry(GO, "Medium", e -> clock.start(Speed.MEDIUM)),
        new Entry(GO, "Fast", e -> clock.start(Speed.FAST)));
  }

  /** Install the menus with the original Swing dialogs and System.exit(0). */
  public static void install(BoardPanel panel, GameController controller, Clock clock) {
    for (Entry entry : entries(controller, clock, new SwingUserDialogs(), () -> System.exit(0))) {
      // Same requesters as the original: the panel owns Grid, the clock owns Go.
      Object requester = entry.menu().equals(GRID) ? panel : clock;
      MenuSite.addLine(requester, entry.menu(), entry.label(), entry.action());
    }
    clock.addClockListener(() -> onTick(controller));
  }

  /** Run the Go menu item with this label. Like the original, any other name stops the clock. */
  static void go(Clock clock, String name) {
    for (Entry entry : goEntries(clock))
      if (entry.label().equals(name)) {
        entry.action().actionPerformed(null);
        return;
      }
    clock.stop();
  }

  /** Advance a generation unless a menu is open. Redrawing is done by the panel's GameListener. */
  static void onTick(GameController controller) {
    if (!menuIsActive()) controller.tick();
  }

  private static boolean menuIsActive() {
    MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
    return path != null && path.length > 0;
  }

  private static void load(GameController controller, UserDialogs dialogs) {
    try {
      controller.load(dialogs.chooseFile("Load"));
    } catch (IOException e) {
      dialogs.showError("Read Failed!");
    }
  }

  private static void store(GameController controller, UserDialogs dialogs) {
    try {
      controller.store(dialogs.chooseFile("Write"));
    } catch (IOException e) {
      dialogs.showError("Write Failed!");
    }
  }
}
