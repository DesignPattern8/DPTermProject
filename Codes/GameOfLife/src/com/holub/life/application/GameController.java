package com.holub.life.application;

import com.holub.life.model.Board;
import com.holub.life.model.BoardSnapshot;
import com.holub.life.model.CellPosition;
import com.holub.life.persistence.BoardFileStore;
import com.holub.life.persistence.LegacyLifeCodec;
import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entry point for the UI's board requests (Facade). Runs the existing Universe model operations,
 * one at a time, and tells {@link GameListener}s when the board changed.
 *
 * <p>Editing, generations, Load and Store all go through {@link Board}. Files are read and written
 * as {@link BoardSnapshot}s by {@link BoardFileStore}, which owns coordinate filtering and file
 * order.
 */
public final class GameController {
  /** Child-block widths of the default {@code Neighborhood(8, Neighborhood(8, Resident))} board. */
  private static final List<Integer> DEFAULT_BLOCK_WIDTHS = List.of(8, 1);

  private final Board board;
  private final Clock clock;
  private final BoardFileStore files;
  private final CancerGame cancerGame; // null: classic Life without cancer rules
  private final Object lock = new Object(); // Board calls must not overlap (model contract)
  private final List<GameListener> listeners = new CopyOnWriteArrayList<>();

  public GameController(Board board, Clock clock) {
    this(board, clock, null);
  }

  /**
   * Runs {@code cancerGame} after every generation. It must act on the board that {@code board}
   * wraps. Files use the default 64x64 board's block layout.
   */
  public GameController(Board board, Clock clock, CancerGame cancerGame) {
    this(board, clock, cancerGame, new BoardFileStore(new LegacyLifeCodec(), DEFAULT_BLOCK_WIDTHS));
  }

  /** {@code files} must be configured with the block layout of {@code board} for Store. */
  public GameController(Board board, Clock clock, CancerGame cancerGame, BoardFileStore files) {
    this.files = Objects.requireNonNull(files, "files");
    this.board = board;
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

  /** Decode and close the input before changing the board or clock; only success emits EDITED. */
  public void load(File file) throws IOException {
    synchronized (lock) {
      BoardSnapshot loaded = files.read(file.toPath(), board.rows(), board.columns());
      clock.stop();
      board.restore(loaded);
    }
    notifyListeners(GameListener.Change.EDITED);
  }

  public void store(File file) throws IOException {
    files.writeSnapshot(
        file.toPath(),
        () -> {
          clock.stop();
          synchronized (lock) {
            return board.snapshot();
          }
        });
  }

  private void notifyListeners(GameListener.Change change) {
    for (GameListener listener : listeners) listener.boardChanged(change);
  }
}
