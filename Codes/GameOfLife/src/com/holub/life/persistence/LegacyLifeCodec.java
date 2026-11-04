package com.holub.life.persistence;

import com.holub.life.model.NeighborhoodState;
import java.awt.Point;
import java.io.*;
import java.util.Collection;

/** The original serialized Collection<Point> format, with Collection<Point> validation. */
public final class LegacyLifeCodec {
  private LegacyLifeCodec() {}

  public static void load(NeighborhoodState state, InputStream in) throws IOException {
    try {
      ObjectInputStream source = new ObjectInputStream(in);
      Object loaded = source.readObject();
      if (!(loaded instanceof Collection))
        throw new IOException("Not a Life file: expected a Collection of cells");
      for (Object cell : (Collection<?>) loaded)
        if (!(cell instanceof Point))
          throw new IOException("Not a Life file: unexpected cell entry");
      state.replaceCells((Collection) loaded);
    } catch (ClassNotFoundException e) {
      throw new IOException("Internal Error: Class not found on load");
    }
  }

  public static void flush(NeighborhoodState state, OutputStream out) throws IOException {
    ObjectOutputStream sink = new ObjectOutputStream(out);
    sink.writeObject(state.liveCells());
  }
}
