package com.holub.life.persistence;

import com.holub.life.model.NeighborhoodState;
import java.awt.Point;
import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * The original serialized Collection<Point> format, with Collection<Point> validation. A board that
 * has cancer cells is written as {@link CancerStateData} instead; a board without cancer cells is
 * still written in the original format.
 */
public final class LegacyLifeCodec {
  private LegacyLifeCodec() {}

  public static void load(NeighborhoodState state, InputStream in) throws IOException {
    try {
      ObjectInputStream source = new ObjectInputStream(in);
      Object loaded = source.readObject();
      if (loaded instanceof CancerStateData data) {
        requirePoints(data.liveCells);
        requirePoints(data.cancerCells);
        state.replaceCells(new LinkedList<Point>(data.liveCells), new HashSet<Point>(data.cancerCells));
        return;
      }
      if (!(loaded instanceof Collection))
        throw new IOException("Not a Life file: expected a Collection of cells");
      requirePoints((Collection<?>) loaded);
      state.replaceCells((Collection) loaded);
    } catch (ClassNotFoundException e) {
      throw new IOException("Internal Error: Class not found on load");
    }
  }

  public static void flush(NeighborhoodState state, OutputStream out) throws IOException {
    ObjectOutputStream sink = new ObjectOutputStream(out);
    if (state.cancerCells().isEmpty()) {
      sink.writeObject(state.liveCells());
    } else {
      sink.writeObject(new CancerStateData(state.liveCells(), state.cancerCells()));
    }
    sink.flush();
  }

  private static void requirePoints(Collection<?> cells) throws IOException {
    if (cells == null) throw new IOException("Not a Life file: missing cell collection");
    for (Object cell : cells)
      if (!(cell instanceof Point)) throw new IOException("Not a Life file: unexpected cell entry");
  }

  /** File format for boards with cancer cells. Cancer cells are also listed in liveCells. */
  private static final class CancerStateData implements Serializable {
    private static final long serialVersionUID = 1L;

    final Collection<Point> liveCells;
    final Set<Point> cancerCells;

    CancerStateData(Collection<Point> liveCells, Set<Point> cancerCells) {
      this.liveCells = new LinkedList<Point>(liveCells);
      this.cancerCells = new HashSet<Point>(cancerCells);
    }
  }
}
