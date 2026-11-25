package com.holub.life.ui;

import com.holub.life.application.GameController;
import com.holub.life.application.PresetResult;
import com.holub.life.model.CellPosition;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The Preset menu's "waiting for a click" state (decision 010). After {@link #select}, the next left
 * click on the board places the preset with its top-left cell there; a right click cancels. Either
 * way the panel goes back to its normal toggle/treat clicks. Used on the Swing event thread.
 */
public final class PresetPlacement {
  static final String DOES_NOT_FIT = "Preset does not fit here.";
  static final String UNKNOWN = "Unknown preset.";

  private final GameController controller;
  private final UserDialogs dialogs;
  private final List<Consumer<Boolean>> waitingListeners = new CopyOnWriteArrayList<>();
  private String pending; // null: not waiting

  public PresetPlacement(GameController controller, UserDialogs dialogs) {
    this.controller = controller;
    this.dialogs = dialogs;
  }

  /** Told {@code true} when waiting for a click starts and {@code false} when it ends. */
  public void addWaitingListener(Consumer<Boolean> listener) {
    waitingListeners.add(listener);
  }

  /** Wait for a click to place preset {@code id}. Replaces any preset already waiting. */
  public void select(String id) {
    boolean wasWaiting = pending != null;
    pending = id;
    if (!wasWaiting) tellWaiting(true);
  }

  /** Stop waiting, if waiting (another menu item, a mode switch). */
  public void cancel() {
    if (pending == null) return;
    pending = null;
    tellWaiting(false);
  }

  public boolean isWaiting() {
    return pending != null;
  }

  /**
   * Handle a board click.
   *
   * @return true if the click was used here (placed, failed or cancelled), so the caller must not
   *     also toggle or treat; false when not waiting.
   */
  public boolean click(CellPosition cell, boolean rightButton) {
    if (pending == null) return false;
    String id = pending;
    cancel(); // decision 010-4 A: one click ends waiting, whatever the result
    if (rightButton) return true; // cancel only, no treatment
    PresetResult result = controller.applyPreset(id, cell);
    if (result == PresetResult.OUT_OF_BOUNDS) dialogs.showError(DOES_NOT_FIT);
    else if (result == PresetResult.UNKNOWN_ID) dialogs.showError(UNKNOWN);
    return true;
  }

  private void tellWaiting(boolean waiting) {
    for (Consumer<Boolean> listener : waitingListeners) listener.accept(waiting);
  }
}
