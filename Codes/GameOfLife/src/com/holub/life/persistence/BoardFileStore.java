package com.holub.life.persistence;

import java.io.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

/** Owns file streams; the caller retains board and clock policy. */
public final class BoardFileStore {
  private final PatternCodec codec;

  public BoardFileStore(PatternCodec codec) {
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  public PatternData read(Path path) throws IOException {
    return read(path, () -> {});
  }

  /** Runs preparation only after opening succeeds, before decoding begins. */
  public PatternData read(Path path, Runnable afterOpen) throws IOException {
    Objects.requireNonNull(afterOpen, "afterOpen");
    try (InputStream in = new FileInputStream(path.toFile())) {
      afterOpen.run();
      return codec.read(in);
    }
  }

  public void write(Path path, PatternData data) throws IOException {
    Objects.requireNonNull(data, "data");
    write(path, () -> data);
  }

  /** Opens/truncates the output before capturing, preserving the legacy Store order. */
  public void write(Path path, Supplier<PatternData> capture) throws IOException {
    Objects.requireNonNull(capture, "capture");
    try (OutputStream out = new FileOutputStream(path.toFile())) {
      codec.write(capture.get(), out);
    }
  }
}
