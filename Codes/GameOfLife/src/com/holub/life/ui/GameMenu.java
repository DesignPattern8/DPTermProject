package com.holub.life.ui;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.application.PresetChoice;
import com.holub.life.application.Speed;
import com.holub.ui.MenuSite;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.*;

/** The original Grid and Go menus as one table of entries, plus the menu-open tick filter. */
public final class GameMenu {
  static final String GRID = "Grid";
  static final String GO = "Go";
  static final String PRESET = "Preset";

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

  /**
   * Install only the Grid and Go menus. The caller decides what a clock tick drives ({@link
   * ModeSwitch}, twoplayer/docs/007).
   */
  public static void installMenus(BoardPanel panel, GameController controller, Clock clock) {
    for (Entry entry : entries(controller, clock, new SwingUserDialogs(), () -> System.exit(0))) {
      // Same requesters as the original: the panel owns Grid, the clock owns Go.
      Object requester = entry.menu().equals(GRID) ? panel : clock;
      MenuSite.addLine(requester, entry.menu(), entry.label(), entry.action());
    }
  }

  /**
   * The Preset menu items, one per {@link GameController#presets()} in catalog order (decision
   * 010). Each starts waiting for a board click. Kept apart from {@link #entries} so the original
   * Grid and Go menus stay as they were.
   */
  public static List<Entry> presetEntries(GameController controller, PresetPlacement placement) {
    List<Entry> result = new ArrayList<>();
    for (PresetChoice choice : controller.presets())
      result.add(new Entry(PRESET, choice.displayName(), e -> placement.select(choice.id())));
    return List.copyOf(result);
  }

  /**
   * Install Grid, Go and Preset. Choosing any Grid or Go item first cancels a preset that is
   * waiting for a click (decision 010, #62 Q4).
   */
  public static void installMenus(
      BoardPanel panel, GameController controller, Clock clock, PresetPlacement placement) {
    for (Entry entry : entries(controller, clock, new SwingUserDialogs(), () -> System.exit(0))) {
      Object requester = entry.menu().equals(GRID) ? panel : clock;
      MenuSite.addLine(
          requester,
          entry.menu(),
          entry.label(),
          e -> {
            placement.cancel();
            entry.action().actionPerformed(e);
          });
    }
    for (Entry entry : presetEntries(controller, placement))
      MenuSite.addLine(panel, entry.menu(), entry.label(), entry.action());
  }

  /** Enable or disable the Preset menu items (disabled in two-player mode, decision 010). */
  public static void setPresetsEnabled(BoardPanel panel, GameController controller, boolean enabled) {
    for (PresetChoice choice : controller.presets()) {
      JMenuItem item = MenuSite.getMyMenuItem(panel, PRESET, choice.displayName());
      if (item != null) item.setEnabled(enabled);
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

  /** Cancelling the chooser does nothing: no load, no error box (#57). */
  private static void load(GameController controller, UserDialogs dialogs) {
    Optional<File> file = dialogs.chooseFile("Load");
    if (file.isEmpty()) return;
    try {
      controller.load(file.get());
    } catch (IOException e) {
      dialogs.showError("Read Failed!");
    }
  }

  /** Cancelling the chooser does nothing: no store, no error box (#57). */
  private static void store(GameController controller, UserDialogs dialogs) {
    Optional<File> file = dialogs.chooseFile("Write");
    if (file.isEmpty()) return;
    try {
      controller.store(file.get());
    } catch (IOException e) {
      dialogs.showError("Write Failed!");
    }
  }
}
