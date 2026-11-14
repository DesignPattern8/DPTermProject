package com.holub.life.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.CellPosition;
import java.awt.Point;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class LegacyLifeCodecTest {
  private final PatternCodec codec = new LegacyLifeCodec();

  private static byte[] serialized(Object value) throws IOException {
    var bytes = new ByteArrayOutputStream();
    try (var out = new ObjectOutputStream(bytes)) {
      out.writeObject(value);
    }
    return bytes.toByteArray();
  }

  @Test
  void roundTripsLegacyCoordinates() throws Exception {
    var data = new PatternData(List.of(new CellPosition(5, 6), new CellPosition(2, 12)), Set.of());
    var bytes = new ByteArrayOutputStream();
    codec.write(data, bytes);
    assertEquals(
        List.of(new Point(6, 5), new Point(12, 2)),
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject());
    assertEquals(data, codec.read(new ByteArrayInputStream(bytes.toByteArray())));
  }

  @Test
  void preservesEmptyAndDuplicateEntries() throws Exception {
    for (var points :
        List.of(List.<Point>of(), List.of(new Point(7, 2), new Point(7, 2), new Point(-1, 1000)))) {
      var data = codec.read(new ByteArrayInputStream(serialized(points)));
      assertEquals(points.stream().map(p -> new CellPosition(p.y, p.x)).toList(), data.cells());
    }
  }

  @Test
  void rejectsMalformedContents() throws Exception {
    for (Object value : Arrays.asList(null, "bad", Arrays.asList((Point) null), List.of("bad"))) {
      byte[] bytes = serialized(value);
      assertThrows(IOException.class, () -> codec.read(new ByteArrayInputStream(bytes)));
    }
    assertThrows(
        IOException.class, () -> codec.read(new ByteArrayInputStream(new byte[] {1, 2, 3})));
  }

  @Test
  void doesNotCloseCallerStreams() throws Exception {
    class Input extends ByteArrayInputStream {
      boolean closed;

      Input(byte[] bytes) {
        super(bytes);
      }

      @Override
      public void close() {
        closed = true;
      }
    }
    class Output extends ByteArrayOutputStream {
      boolean closed;

      @Override
      public void close() {
        closed = true;
      }
    }
    for (byte[] bytes : List.of(serialized(List.of(new Point(1, 2))), new byte[] {1})) {
      var in = new Input(bytes);
      try {
        codec.read(in);
      } catch (IOException expected) {
      }
      assertFalse(in.closed);
    }
    var out = new Output();
    codec.write(new PatternData(List.of(), Set.of()), out);
    assertFalse(out.closed);
    var failed =
        new OutputStream() {
          @Override
          public void write(int value) throws IOException {
            throw new IOException("write failed");
          }

          @Override
          public void close() {
            fail("Codec closed caller output");
          }
        };
    assertThrows(
        IOException.class, () -> codec.write(new PatternData(List.of(), Set.of()), failed));
  }

  @Test
  void readsPreRefactoringCancerFixture() throws Exception {
    // Generated with origin/main 4435a31 LegacyLifeCodec before this refactoring.
    byte[] fixture =
        Base64.getDecoder()
            .decode(
                "rO0ABXNyADpjb20uaG9sdWIubGlmZS5wZXJzaXN0ZW5jZS5MZWdhY3lMaWZlQ29kZWMkQ2FuY2VyU3RhdGVEYXRhAAAAAAAAAAECAAJMAAtjYW5jZXJDZWxsc3QAD0xqYXZhL3V0aWwvU2V0O0wACWxpdmVDZWxsc3QAFkxqYXZhL3V0aWwvQ29sbGVjdGlvbjt4cHNyABFqYXZhLnV0aWwuSGFzaFNldLpEhZWWuLc0AwAAeHB3DAAAABA/QAAAAAAAAXNyAA5qYXZhLmF3dC5Qb2ludLbEinI0fsgmAgACSQABeEkAAXl4cAAAAAIAAAADeHNyABRqYXZhLnV0aWwuTGlua2VkTGlzdAwpU11KYIgiAwAAeHB3BAAAAAJzcQB+AAYAAAAGAAAABXNxAH4ABgAAAAIAAAADeA==");
    var data = codec.read(new ByteArrayInputStream(fixture));
    assertEquals(List.of(new CellPosition(5, 6), new CellPosition(3, 2)), data.cells());
    assertEquals(Set.of(new CellPosition(3, 2)), data.cancerCells());
    var out = new ByteArrayOutputStream();
    codec.write(data, out);
    assertEquals(data, codec.read(new ByteArrayInputStream(out.toByteArray())));
  }
}
