package com.holub.life.ui;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.application.Speed;
import com.holub.ui.MenuSite;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
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
    return entries(() -> controller, clock, dialogs, exit);
  }

  /**
   * Same table, but Clear, Load and Store act on whichever controller {@code active} returns when
   * the item is chosen: the current mode's board (Classic or Single Player, ui/docs/010).
   */
  public static List<Entry> entries(
      Supplier<GameController> active, Clock clock, UserDialogs dialogs, Runnable exit) {
    List<Entry> entries =
        new ArrayList<>(
            List.of(
                new Entry(GRID, "Clear", e -> active.get().clear()),
                new Entry(GRID, "Load", e -> load(active.get(), dialogs)),
                new Entry(GRID, "Store", e -> store(active.get(), dialogs)),
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

  /**
   * Install the menus with the original Swing dialogs and System.exit(0), and a clock listener
   * that advances {@code controller} on every tick (the original behaviour).
   */
  public static void install(BoardPanel panel, GameController controller, Clock clock) {
    installMenus(panel, controller, clock);
    clock.addClockListener(() -> onTick(controller));
  }

  /**
   * Install only the Grid and Go menus. The caller decides what a clock tick drives ({@link
   * ModeSwitch}, twoplayer/docs/007).
   */
  public static void installMenus(BoardPanel panel, GameController controller, Clock clock) {
    installMenus(panel, () -> controller, clock);
  }

  /** Like {@link #installMenus(BoardPanel, GameController, Clock)}, routed to {@code active}. */
  public static void installMenus(BoardPanel panel, Supplier<GameController> active, Clock clock) {
    for (Entry entry : entries(active, clock, new SwingUserDialogs(), () -> System.exit(0))) {
      // Same requesters as the original: the panel owns Grid, the clock owns Go.
      Object requester = entry.menu().equals(GRID) ? panel : clock;
      MenuSite.addLine(requester, entry.menu(), entry.label(), entry.action());
    }
  }

  /** Enable or disable Grid's Clear, Load and Store (disabled in two-player mode). Exit stays. */
  public static void setFileCommandsEnabled(BoardPanel panel, boolean enabled) {
    for (String name : new String[] {"Clear", "Load", "Store"}) {
      JMenuItem item = MenuSite.getMyMenuItem(panel, GRID, name);
      if (item != null) item.setEnabled(enabled);
    }
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

  static boolean menuIsActive() {
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
