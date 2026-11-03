package com.holub.life.application;

import com.holub.life.model.Cell;
import com.holub.life.model.NeighborhoodState;
import com.holub.life.persistence.BoardFileStore;
import com.holub.life.persistence.LegacyLifeCodec;
import java.awt.Point;
import java.io.*;

/** Existing Universe model operations, without Swing event or dialog handling. */
public final class GameController {
  private final Cell outermostCell;
  private final Clock clock;

  public GameController(Cell outermostCell, Clock clock) {
    this.outermostCell = outermostCell;
    this.clock = clock;
  }

  public void toggle(int row, int column) {
    outermostCell.toggle(row, column);
  }

  public void clear() {
    outermostCell.clear();
  }

  public boolean tick() {
    if (outermostCell.figureNextState(
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY,
        Cell.DUMMY)) {
      return outermostCell.transition();
    }
    return false;
  }

  public void load(File file) throws IOException {
    InputStream in = BoardFileStore.openForLoad(file);
    clock.stop();
    outermostCell.clear();
    NeighborhoodState memento = outermostCell.createMemento();
    LegacyLifeCodec.load(memento, in);
    outermostCell.transfer(memento, new Point(0, 0), Cell.LOAD);
    in.close();
  }

  public void store(File file) throws IOException {
    OutputStream out = BoardFileStore.openForStore(file);
    clock.stop();
    NeighborhoodState memento = outermostCell.createMemento();
    outermostCell.transfer(memento, new Point(0, 0), Cell.STORE);
    LegacyLifeCodec.flush(memento, out);
    out.close();
  }
}
