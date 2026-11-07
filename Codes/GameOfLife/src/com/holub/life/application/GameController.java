package com.holub.life.application;

import com.holub.life.model.Cell;
import com.holub.life.model.NeighborhoodState;
import com.holub.life.persistence.BoardFileStore;
import com.holub.life.persistence.LegacyLifeCodec;
import java.awt.Point;
import java.io.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entry point for the UI's board requests (Facade). Runs the existing Universe model operations and
 * tells {@link GameListener}s when the board changed.
 */
public final class GameController {
  private final Cell outermostCell;
  private final Clock clock;
  private final List<GameListener> listeners = new CopyOnWriteArrayList<>();

  public GameController(Cell outermostCell, Clock clock) {
    this.outermostCell = outermostCell;
    this.clock = clock;
  }

  public void addGameListener(GameListener listener) {
    listeners.add(listener);
  }

  public void toggle(int row, int column) {
    outermostCell.toggle(row, column);
    notifyListeners(GameListener.Change.EDITED);
  }

  public void clear() {
    outermostCell.clear();
    notifyListeners(GameListener.Change.EDITED);
  }

  /**
   * Advance one generation.
   *
   * @return true if any cell changed; listeners are notified only in that case.
   */
  public boolean tick() {
    boolean changed = false;
    if (outermostCell.figureNextState(
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY)) {
      changed = outermostCell.transition();
    }
    if (changed) notifyListeners(GameListener.Change.GENERATION);
    return changed;
  }

  /** Listeners are notified even when loading fails, as the original redrew after a failed load. */
  public void load(File file) throws IOException {
    try (InputStream in = BoardFileStore.openForLoad(file)) {
      clock.stop();
      outermostCell.clear();
      NeighborhoodState memento = outermostCell.createMemento();
      LegacyLifeCodec.load(memento, in);
      outermostCell.transfer(memento, new Point(0, 0), Cell.LOAD);
    } finally {
      notifyListeners(GameListener.Change.EDITED);
    }
  }

  public void store(File file) throws IOException {
    try (OutputStream out = BoardFileStore.openForStore(file)) {
      clock.stop();
      NeighborhoodState memento = outermostCell.createMemento();
      LegacyLifeCodec.flush(memento, out);
    }
  }

  private void notifyListeners(GameListener.Change change) {
    for (GameListener listener : listeners) listener.boardChanged(change);
  }
}
