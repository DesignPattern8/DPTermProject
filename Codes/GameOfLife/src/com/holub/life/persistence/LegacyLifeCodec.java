package com.holub.life.persistence;

import com.holub.life.model.CellPosition;
import java.awt.Point;
import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/** Reads both legacy Collection<Point> files and the existing cancer extension. */
public final class LegacyLifeCodec implements PatternCodec {
  @Override
  public PatternData read(InputStream in) throws IOException {
    try {
      Object loaded = new ObjectInputStream(in).readObject();
      if (loaded instanceof CancerStateData data) {
        return new PatternData(
            positions(data.liveCells), new HashSet<>(positions(data.cancerCells)));
      }
      if (!(loaded instanceof Collection<?> cells)) {
        throw new IOException("Not a Life file: expected a Collection of cells");
      }
      return new PatternData(positions(cells), Set.of());
    } catch (ClassNotFoundException e) {
      throw new IOException("Internal Error: Class not found on load", e);
    }
  }

  @Override
  public void write(PatternData data, OutputStream out) throws IOException {
    ObjectOutputStream sink = new ObjectOutputStream(out);
    Collection<Point> live = points(data.cells());
    if (data.cancerCells().isEmpty()) {
      sink.writeObject(live);
    } else {
      sink.writeObject(new CancerStateData(live, new HashSet<>(points(data.cancerCells()))));
    }
    sink.flush();
  }

  private static List<CellPosition> positions(Collection<?> cells) throws IOException {
    if (cells == null) throw new IOException("Not a Life file: missing cell collection");
    List<CellPosition> positions = new java.util.ArrayList<>();
    for (Object cell : cells) {
      if (!(cell instanceof Point point)) {
        throw new IOException("Not a Life file: unexpected cell entry");
      }
      positions.add(new CellPosition(point.y, point.x));
    }
    return positions;
  }

  private static Collection<Point> points(Collection<CellPosition> cells) {
    Collection<Point> points = new LinkedList<>();
    for (CellPosition cell : cells) points.add(new Point(cell.column(), cell.row()));
    return points;
  }

  /** Keep this binary name, serialVersionUID and field types compatible with PR #60 files. */
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
