package com.holub.life.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Encodes file data without closing streams owned by the caller. */
public interface PatternCodec {
  PatternData read(InputStream in) throws IOException;

  void write(PatternData data, OutputStream out) throws IOException;
}
