package com.holub.life.persistence;

import com.holub.life.NeighborhoodState;
import java.io.*;
import java.util.Collection;

/** The original serialized Collection<Point> format, including its unchecked load behavior. */
public final class LegacyLifeCodec {
  private LegacyLifeCodec() {}

  public static void load(NeighborhoodState state, InputStream in) throws IOException {
    try {
      ObjectInputStream source = new ObjectInputStream(in);
      state.replaceCells((Collection) source.readObject());
    } catch (ClassNotFoundException e) {
      throw new IOException("Internal Error: Class not found on load");
    }
  }

  public static void flush(NeighborhoodState state, OutputStream out) throws IOException {
    ObjectOutputStream sink = new ObjectOutputStream(out);
    sink.writeObject(state.liveCells());
  }
}
