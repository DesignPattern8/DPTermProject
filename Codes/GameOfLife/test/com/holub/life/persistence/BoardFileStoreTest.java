package com.holub.life.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoardFileStoreTest {
  @TempDir Path directory;
  private static final PatternData EMPTY = new PatternData(List.of(), Set.of());

  @Test
  void closesStreamsOnSuccessAndFailure() throws Exception {
    for (boolean fail : List.of(false, true)) {
      var codec =
          new PatternCodec() {
            InputStream input;
            OutputStream output;

            public PatternData read(InputStream in) throws IOException {
              input = in;
              if (fail) throw new IOException("read failed");
              return EMPTY;
            }

            public void write(PatternData data, OutputStream out) throws IOException {
              output = out;
              if (fail) throw new IOException("write failed");
            }
          };
      var store = new BoardFileStore(codec);
      var path = directory.resolve("board.life");
      Files.write(path, new byte[] {1});
      if (fail) assertThrows(IOException.class, () -> store.read(path));
      else assertEquals(EMPTY, store.read(path));
      assertThrows(IOException.class, () -> codec.input.read());
      if (fail) assertThrows(IOException.class, () -> store.write(path, EMPTY));
      else store.write(path, EMPTY);
      assertThrows(IOException.class, () -> codec.output.write(1));
    }
  }

  @Test
  void doesNotPrepareWhenOpenFails() {
    var store = new BoardFileStore(new LegacyLifeCodec());
    assertThrows(
        FileNotFoundException.class,
        () -> store.read(directory.resolve("missing"), () -> fail("prepared before open")));
    assertThrows(
        IOException.class,
        () ->
            store.write(
                directory,
                () -> {
                  fail("captured before open");
                  return EMPTY;
                }));
  }

  @Test
  void preparesBeforeDecodingAndCapturesBeforeEncoding() throws Exception {
    var events = new ArrayList<String>();
    var store =
        new BoardFileStore(
            new PatternCodec() {
              public PatternData read(InputStream in) {
                events.add("decode");
                return EMPTY;
              }

              public void write(PatternData data, OutputStream out) {
                events.add("encode");
              }
            });
    var file = directory.resolve("board.life");
    Files.write(file, new byte[0]);
    store.read(file, () -> events.add("prepare"));
    store.write(
        file,
        () -> {
          events.add("capture");
          return EMPTY;
        });
    assertEquals(List.of("prepare", "decode", "capture", "encode"), events);
  }

  @Test
  void closesStreamsWhenPreparationFails() throws Exception {
    var store = new BoardFileStore(new LegacyLifeCodec());
    var file = directory.resolve("board.life");
    Files.write(file, new byte[0]);
    var failure = new IllegalStateException("preparation failed");
    long before = descriptors();
    for (int i = 0; i < 20; i++) {
      assertSame(
          failure,
          assertThrows(
              IllegalStateException.class,
              () ->
                  store.read(
                      file,
                      () -> {
                        throw failure;
                      })));
      assertSame(
          failure,
          assertThrows(
              IllegalStateException.class,
              () ->
                  store.write(
                      file,
                      () -> {
                        throw failure;
                      })));
    }
    if (before >= 0) assertTrue(descriptors() <= before);
    Files.delete(file);
  }

  private static long descriptors() {
    var os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
    return os instanceof com.sun.management.UnixOperatingSystemMXBean unix
        ? unix.getOpenFileDescriptorCount()
        : -1;
  }
}
