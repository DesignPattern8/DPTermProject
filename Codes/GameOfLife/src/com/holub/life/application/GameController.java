package com.holub.life.application;

import com.holub.life.model.Board;
import com.holub.life.model.Cell;
import com.holub.life.model.CellPosition;
import com.holub.life.model.NeighborhoodState;
import com.holub.life.persistence.BoardFileStore;
import com.holub.life.persistence.LegacyLifeCodec;
import java.awt.Point;
import java.io.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entry point for the UI's board requests (Facade). Runs the existing Universe model operations,
 * one at a time, and tells {@link GameListener}s when the board changed.
 *
 * <p>Editing and generations go through {@link Board}. Load and Store still use the legacy {@link
 * Cell} memento path that {@code LegacyLifeCodec} reads and writes; {@code fileCells} must be the
 * same board that {@code board} wraps.
 */
public final class GameController {
  private final Board board;
  private final Cell fileCells;
  private final Clock clock;
  private final CancerGame cancerGame; // null: classic Life without cancer rules
  private final Object lock = new Object(); // Board calls must not overlap (model contract)
  private final List<GameListener> listeners = new CopyOnWriteArrayList<>();

  public GameController(Board board, Cell fileCells, Clock clock) {
    this(board, fileCells, clock, null);
  }

  /** Runs {@code cancerGame} after every generation. It must act on the board that {@code board} wraps. */
  public GameController(Board board, Cell fileCells, Clock clock, CancerGame cancerGame) {
    this.board = board;
    this.fileCells = fileCells;
    this.clock = clock;
    this.cancerGame = cancerGame;
  }

  public void addGameListener(GameListener listener) {
    listeners.add(listener);
  }

  public void toggle(int row, int column) {
    synchronized (lock) {
      board.toggle(new CellPosition(row, column));
    }
    notifyListeners(GameListener.Change.EDITED);
  }

  public void clear() {
    synchronized (lock) {
      board.clear();
    }
    notifyListeners(GameListener.Change.EDITED);
  }

  /**
   * Advance one generation.
   *
   * @return true if any cell changed; listeners are notified only in that case.
   */
  public boolean tick() {
    if (cancerGame == null) {
      boolean changed;
      synchronized (lock) {
        changed = board.step();
      }
      if (changed) notifyListeners(GameListener.Change.GENERATION);
      return changed;
    }

    boolean changed;
    boolean ended;
    synchronized (lock) {
      if (cancerGame.isOver()) return false;
      changed = board.step();
      ended = cancerGame.afterGeneration();
    }
    if (changed) notifyListeners(GameListener.Change.GENERATION);
    // Cancer rules change cells outside step(), so redraw the whole board as Universe did.
    notifyListeners(GameListener.Change.EDITED);
    if (ended) {
      clock.stop();
      notifyListeners(GameListener.Change.GAME_OVER);
    }
    return changed;
  }

  /**
   * Treat cancer around (row, column). Does nothing without cancer rules or treatment charges.
   *
   * @return true if a treatment was used.
   */
  public boolean treat(int row, int column) {
    if (cancerGame == null) return false;
    boolean treated;
    synchronized (lock) {
      treated = cancerGame.treat(row, column);
    }
    if (treated) notifyListeners(GameListener.Change.EDITED);
    return treated;
  }

  /** The cancer game's win/lose message, or null if the game is running or has no cancer rules. */
  public String gameResultMessage() {
    return cancerGame == null ? null : cancerGame.resultMessage();
  }

  /** Listeners are notified even when loading fails, as the original redrew after a failed load. */
  public void load(File file) throws IOException {
    try (InputStream in = BoardFileStore.openForLoad(file)) {
      clock.stop();
      synchronized (lock) {
        fileCells.clear();
        NeighborhoodState memento = fileCells.createMemento();
        LegacyLifeCodec.load(memento, in);
        fileCells.transfer(memento, new Point(0, 0), Cell.LOAD);
      }
    } finally {
      notifyListeners(GameListener.Change.EDITED);
    }
  }

  public void store(File file) throws IOException {
    try (OutputStream out = BoardFileStore.openForStore(file)) {
      clock.stop();
      NeighborhoodState memento;
      synchronized (lock) {
        memento = fileCells.createMemento();
      }
      LegacyLifeCodec.flush(memento, out);
    }
  }

  private void notifyListeners(GameListener.Change change) {
    for (GameListener listener : listeners) listener.boardChanged(change);
  }
}
