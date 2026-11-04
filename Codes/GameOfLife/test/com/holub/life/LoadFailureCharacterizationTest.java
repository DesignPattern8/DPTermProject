package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoadFailureCharacterizationTest {
  @Test
  void invalidFileStillClearsBoard() {
    ClassicBoard board = new ClassicBoard().click(5, 5);
    assertThrows(IOException.class, () -> board.load(new byte[] {1, 2, 3, 4}));
    assertTrue(board.liveCells().isEmpty());
  }

  @Test
  void nonPointElementsThrowIOException() throws Exception {
    ClassicBoard board = new ClassicBoard().click(5, 5);
    byte[] bytes = serialized(List.of("not a Point"));
    assertThrows(IOException.class, () -> board.load(bytes));
    assertTrue(board.liveCells().isEmpty());
  }

  @Test
  void nullCollectionThrowsIOException() throws Exception {
    ClassicBoard board = new ClassicBoard().click(5, 5);
    byte[] bytes = serialized(null);
    assertThrows(IOException.class, () -> board.load(bytes));
    assertTrue(board.liveCells().isEmpty());
  }

  private static byte[] serialized(Object value) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream stream = new ObjectOutputStream(bytes)) {
      stream.writeObject(value);
    }
    return bytes.toByteArray();
  }
}
