package com.holub.life.application;

/**
 * Notified by {@link GameController} after it changes the board. Generation changes are reported
 * on the thread that ran the tick (the Timer thread for {@link UtilTimerTickSource}); edits are
 * reported on the caller's thread.
 */
public interface GameListener {
  /** What kind of redraw the change needs. */
  enum Change {
    /** Toggle, Clear or Load: redraw the whole board. */
    EDITED,
    /** A tick that changed at least one cell: redraw only the changed blocks. */
    GENERATION
  }

  void boardChanged(Change change);
}
