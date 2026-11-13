package com.holub.life.model;

/** Classic board operations. Callers must serialize access to a board. */
public interface Board {
  int rows();

  int columns();

  /** Advances one generation and reports whether any cell changed. */
  boolean step();

  void toggle(CellPosition position);

  void clear();

  /** Captures the current layout as an immutable value. */
  BoardSnapshot snapshot();

  /** Replaces the entire layout with a snapshot of the same dimensions. */
  void restore(BoardSnapshot snapshot);
}
