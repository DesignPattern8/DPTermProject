package com.holub.life.model;

/** Classic and cancer board operations. Callers must serialize access to a board. */
public interface Board {
  int rows();

  int columns();

  /** Advances one generation and reports whether any cell changed. */
  boolean step();

  void toggle(CellPosition position);

  void clear();

  /** Captures live and cancer cell positions as an immutable value. */
  BoardSnapshot snapshot();

  /** Replaces live and cancer cell states with a snapshot of the same dimensions. */
  void restore(BoardSnapshot snapshot);
}
