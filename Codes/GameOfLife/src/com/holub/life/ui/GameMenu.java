package com.holub.life.ui;

import com.holub.life.application.GameController;
import com.holub.ui.MenuSite;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The Grid and Go menus. Each line is an explicit (menu, label, action) entry, so the label no
 * longer decides the behavior (the original Clock picked the speed from the first letter of the
 * menu item's name). The actions only forward requests to the controller; running and undoing
 * commands is the controller's job.
 *
 * <p>Replaces the menu setup in the original Universe constructor and Clock.createMenus().
 */
public final class GameMenu {
  /** One menu line. */
  public static final class Entry {
    public final String menu;
    public final String label;
    public final Runnable action;

    Entry(String menu, String label, Runnable action) {
      this.menu = menu;
      this.label = label;
      this.action = action;
    }
  }

  static final String GRID = "Grid";
  static final String GO = "Go";

  static final String READ_FAILED = "Read Failed!";
  static final String WRITE_FAILED = "Write Failed!";

  // Milliseconds between generations, same values as the original Clock.
  static final int AGONIZING = 500;
  static final int SLOW = 150;
  static final int MEDIUM = 70;
  static final int FAST = 30;

  private final GameController controller;
  private final FileDialogs dialogs;
  private final Runnable exit;

  public GameMenu(GameController controller, FileDialogs dialogs) {
    this(controller, dialogs, () -> System.exit(0));
  }

  GameMenu(GameController controller, FileDialogs dialogs, Runnable exit) {
    this.controller = controller;
    this.dialogs = dialogs;
    this.exit = exit;
  }

  /** The menu lines, in the order they appear on the menu bar. */
  public List<Entry> entries() {
    return List.of(
        new Entry(GRID, "Clear", controller::clear),
        new Entry(GRID, "Load", this::load),
        new Entry(GRID, "Store", this::store),
        new Entry(GRID, "Exit", exit),
        new Entry(GO, "Halt", controller::stop),
        new Entry(GO, "Tick (Single Step)", controller::step),
        new Entry(GO, "Agonizing", () -> controller.start(AGONIZING)),
        new Entry(GO, "Slow", () -> controller.start(SLOW)),
        new Entry(GO, "Medium", () -> controller.start(MEDIUM)),
        new Entry(GO, "Fast", () -> controller.start(FAST)));
  }

  /** Puts every entry on the menu bar. MenuSite.establish() must have been called already. */
  public void install() {
    for (Entry entry : entries())
      MenuSite.addLine(this, entry.menu, entry.label, e -> entry.action.run());
  }

  void load() {
    // The original code reported a cancelled dialog as a read failure, too.
    Optional<File> file = dialogs.chooseLifeFile("Load");
    if (file.isEmpty()) {
      dialogs.showError(READ_FAILED);
      return;
    }
    try {
      controller.load(file.get());
    } catch (IOException e) {
      dialogs.showError(READ_FAILED);
    }
  }

  void store() {
    Optional<File> file = dialogs.chooseLifeFile("Write");
    if (file.isEmpty()) {
      dialogs.showError(WRITE_FAILED);
      return;
    }
    try {
      controller.store(file.get());
    } catch (IOException e) {
      dialogs.showError(WRITE_FAILED);
    }
  }
}
