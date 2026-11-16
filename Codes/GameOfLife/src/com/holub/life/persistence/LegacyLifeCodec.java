package com.holub.life.persistence;

import com.holub.life.model.CellPosition;
import java.awt.Point;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Reads both legacy Collection<Point> files and the existing cancer extension. */
public final class LegacyLifeCodec implements PatternCodec {
  private static final int MAX_BYTES = 1_048_576;
  private static final int MAX_DEPTH = 8;
  private static final int MAX_REFERENCES = 65_536;
  private static final int MAX_ARRAY_LENGTH = 32_768;
  private static final int MAX_CELLS = 16_384;
  private static final Set<Class<?>> ALLOWED_CLASSES =
      Set.of(
          Point.class,
          CancerStateData.class,
          LinkedList.class,
          ArrayList.class,
          HashSet.class,
          Object[].class,
          Point[].class,
          Map.Entry[].class);
  private static final Set<String> JDK_COLLECTION_CLASSES =
      Set.of(
          "java.util.CollSer",
          "java.util.Arrays$ArrayList",
          "java.util.Collections$EmptyList",
          "java.util.Collections$SingletonList",
          "java.util.ImmutableCollections$List12",
          "java.util.ImmutableCollections$ListN");

  @Override
  public PatternData read(InputStream in) throws IOException {
    try {
      ObjectInputStream source = new ObjectInputStream(new LimitedInputStream(in));
      ObjectInputFilter existing = source.getObjectInputFilter();
      ObjectInputFilter policy = LegacyLifeCodec::checkInput;
      source.setObjectInputFilter(
          existing == null ? policy : ObjectInputFilter.merge(existing, policy));
      Object loaded = source.readObject();
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
    requireCellCount(data.cells().size());
    requireCellCount(data.cancerCells().size());
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
    requireCellCount(cells.size());
    List<CellPosition> positions = new ArrayList<>();
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

  private static void requireCellCount(int size) throws IOException {
    if (size > MAX_CELLS) throw new IOException("Life file exceeds " + MAX_CELLS + " cell entries");
  }

  private static ObjectInputFilter.Status checkInput(ObjectInputFilter.FilterInfo info) {
    if (info.depth() > MAX_DEPTH
        || info.references() > MAX_REFERENCES
        || info.arrayLength() > MAX_ARRAY_LENGTH
        || info.streamBytes() > MAX_BYTES) {
      return ObjectInputFilter.Status.REJECTED;
    }
    Class<?> type = info.serialClass();
    if (type == null) return ObjectInputFilter.Status.UNDECIDED;
    boolean allowed =
        ALLOWED_CLASSES.contains(type)
            || (type.getModule() == List.class.getModule()
                && JDK_COLLECTION_CLASSES.contains(type.getName()));
    return allowed ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
  }

  /** Filters do not see every String or primitive block; bound the actual read/skip budget too. */
  private static final class LimitedInputStream extends FilterInputStream {
    private long remaining = MAX_BYTES;

    LimitedInputStream(InputStream in) {
      super(Objects.requireNonNull(in, "in"));
    }

    @Override
    public int read() throws IOException {
      requireRemaining();
      int value = in.read();
      if (value >= 0) remaining--;
      return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
      Objects.checkFromIndexSize(offset, length, bytes.length);
      if (length == 0) return 0;
      requireRemaining();
      int count = in.read(bytes, offset, (int) Math.min(length, remaining));
      if (count > 0) remaining -= count;
      return count;
    }

    @Override
    public long skip(long count) throws IOException {
      if (count <= 0) return 0;
      requireRemaining();
      long skipped = in.skip(Math.min(count, remaining));
      remaining -= skipped;
      return skipped;
    }

    @Override
    public int available() throws IOException {
      return (int) Math.min(in.available(), remaining);
    }

    @Override
    public boolean markSupported() {
      return false;
    }

    @Override
    public void mark(int limit) {}

    @Override
    public void reset() throws IOException {
      throw new IOException("Mark/reset is not supported");
    }

    private void requireRemaining() throws IOException {
      if (remaining == 0) throw new IOException("Life input exceeds " + MAX_BYTES + " bytes");
    }
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
