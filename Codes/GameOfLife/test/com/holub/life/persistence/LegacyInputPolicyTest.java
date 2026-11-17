package com.holub.life.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.CellPosition;
import java.awt.Point;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;

class LegacyInputPolicyTest {
  private static final int MAX_BYTES = 1_048_576;
  private static final int MAX_CELLS = 16_384;
  private final PatternCodec codec = new LegacyLifeCodec();

  @Test
  void readsOriginalWriterFixture() throws Exception {
    // Given: e38ce7a Neighborhood$NeighborhoodState.markAsAlive/flush, JDK 26.
    byte[] bytes =
        Base64.getDecoder()
            .decode(
                "rO0ABXNyABRqYXZhLnV0aWwuTGlua2VkTGlzdAwpU11KYIgiAwAAeHB3BAAAAANzcgAOamF2YS5hd3QuUG9pbnS2xIpyNH7IJgIAAkkAAXhJAAF5eHAAAAAGAAAABXNxAH4AAv////8AAAPoc3EAfgACAAAABgAAAAV4");
    // When
    var data = codec.read(new ByteArrayInputStream(bytes));
    // Then
    assertEquals(
        List.of(new CellPosition(5, 6), new CellPosition(1000, -1), new CellPosition(5, 6)),
        data.cells());
  }

  @Test
  void readsSupportedCollections() throws Exception {
    // Given
    Point point = new Point(6, 5);
    List<Collection<Point>> collections =
        List.of(
            new LinkedList<>(List.of(point, point)),
            new ArrayList<>(List.of(point, point)),
            new HashSet<>(List.of(point)),
            Arrays.asList(point, point),
            List.of(point, point),
            List.of(point, point, point),
            List.of(),
            Collections.emptyList(),
            Collections.singletonList(point));
    for (var cells : collections) {
      // When
      var data = codec.read(new ByteArrayInputStream(serialized(cells)));
      // Then
      assertEquals(cells.stream().map(p -> new CellPosition(p.y, p.x)).toList(), data.cells());
    }
  }

  private static final class Marker implements Serializable {
    private static final long serialVersionUID = 1L;
    static int reads;

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      reads++;
      in.defaultReadObject();
    }
  }

  private static final class PointSubclass extends Point {
    private static final long serialVersionUID = 1L;
    static int reads;

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      reads++;
      in.defaultReadObject();
    }
  }

  @Test
  void rejectsUnknownClassBeforeReadObject() throws Exception {
    // Given
    Marker.reads = 0;
    byte[] bytes = serialized(new LinkedList<>(List.of(new Marker())));
    // When
    assertThrows(IOException.class, () -> codec.read(new ByteArrayInputStream(bytes)));
    // Then
    assertEquals(0, Marker.reads, "untrusted readObject ran before validation");
  }

  @Test
  void rejectsPointSubclassBeforeReadObject() throws Exception {
    // Given
    PointSubclass.reads = 0;
    byte[] bytes = serialized(new LinkedList<>(List.of(new PointSubclass())));
    // When
    assertThrows(IOException.class, () -> codec.read(new ByteArrayInputStream(bytes)));
    // Then
    assertEquals(0, PointSubclass.reads);
  }

  @Test
  void rejectsUnsupportedCollection() throws Exception {
    byte[] bytes = serialized(new Vector<>(List.of(new Point(1, 2))));
    assertThrows(InvalidClassException.class, () -> codec.read(new ByteArrayInputStream(bytes)));
  }

  @Test
  void boundsArrayAllocation() throws Exception {
    // Given / When / Then: legal size reaches semantic validation; oversized array is rejected
    // before allocation.
    IOException boundary =
        assertThrows(
            IOException.class,
            () -> codec.read(new ByteArrayInputStream(serialized(new Object[32_768]))));
    assertFalse(boundary instanceof InvalidClassException);
    assertThrows(
        InvalidClassException.class,
        () -> codec.read(new ByteArrayInputStream(serialized(new Object[32_769]))));
  }

  @Test
  void boundsGraphDepth() throws Exception {
    IOException boundary =
        assertThrows(
            IOException.class, () -> codec.read(new ByteArrayInputStream(serialized(nested(8)))));
    assertFalse(boundary instanceof InvalidClassException);
    assertThrows(
        InvalidClassException.class,
        () -> codec.read(new ByteArrayInputStream(serialized(nested(9)))));
  }

  @Test
  void boundsRepeatedReferences() throws Exception {
    // Given: JDK 26 LinkedList<Point> with a repeated Point has max references = entries + 3.
    byte[] boundary = serialized(new LinkedList<>(Collections.nCopies(65_533, new Point(1, 2))));
    byte[] over = serialized(new LinkedList<>(Collections.nCopies(65_534, new Point(1, 2))));
    // When / Then: the allowed graph reaches the separate cell count check.
    IOException countFailure =
        assertThrows(IOException.class, () -> codec.read(new ByteArrayInputStream(boundary)));
    assertFalse(countFailure instanceof InvalidClassException);
    assertThrows(InvalidClassException.class, () -> codec.read(new ByteArrayInputStream(over)));
  }

  @Test
  void boundsBytesConsumed() throws Exception {
    // Given: reset tokens pad an otherwise valid empty LinkedList without allocating objects.
    byte[] boundary = paddedEmptyList(MAX_BYTES);
    byte[] over = paddedEmptyList(MAX_BYTES + 1);
    // When / Then
    assertTrue(codec.read(new ByteArrayInputStream(boundary)).cells().isEmpty());
    assertThrows(IOException.class, () -> codec.read(new ByteArrayInputStream(over)));
  }

  @Test
  void boundsUnfilteredStringsWithoutClosingInput() throws Exception {
    // Given: String instances do not invoke ObjectInputFilter.
    byte[] bytes = serialized("a".repeat(MAX_BYTES + 100));
    var input = new TrackedInput(bytes);
    // When
    assertThrows(IOException.class, () -> codec.read(input));
    // Then
    assertTrue(input.consumed() <= MAX_BYTES, "unbounded String was read");
    assertFalse(input.closed);
  }

  @Test
  void countsSkippedClassAnnotationsAgainstByteLimit() throws Exception {
    // Given: primitive class annotations are skipped by ObjectInputStream.
    var bytes = new ByteArrayOutputStream();
    try (var out =
        new ObjectOutputStream(bytes) {
          @Override
          protected void annotateClass(Class<?> type) throws IOException {
            write(new byte[MAX_BYTES + 100]);
          }
        }) {
      out.writeObject(new LinkedList<Point>());
    }
    var input = new TrackedInput(bytes.toByteArray());
    // When / Then
    assertThrows(IOException.class, () -> codec.read(input));
    assertTrue(input.consumed() <= MAX_BYTES);
    assertFalse(input.closed);
  }

  @Test
  void roundTripsLargestSupportedCancerPattern() throws Exception {
    // Given
    List<CellPosition> cells = new ArrayList<>();
    for (int i = 0; i < MAX_CELLS; i++) cells.add(new CellPosition(i / 64, i % 64));
    var pattern = new PatternData(cells, new HashSet<>(cells));
    var out = new ByteArrayOutputStream();
    // When
    codec.write(pattern, out);
    // Then
    assertEquals(pattern, codec.read(new ByteArrayInputStream(out.toByteArray())));
  }

  @Test
  void rejectsOversizedCellCollections() throws Exception {
    // Given
    byte[] bytes =
        serialized(new LinkedList<>(Collections.nCopies(MAX_CELLS + 1, new Point(1, 2))));
    // When / Then
    assertThrows(IOException.class, () -> codec.read(new ByteArrayInputStream(bytes)));
    var out = new ByteArrayOutputStream();
    out.write(7);
    var tooMany =
        new PatternData(Collections.nCopies(MAX_CELLS + 1, new CellPosition(2, 1)), Set.of());
    assertThrows(IOException.class, () -> codec.write(tooMany, out));
    assertArrayEquals(new byte[] {7}, out.toByteArray());
    var cancer = new HashSet<CellPosition>();
    for (int i = 0; i <= MAX_CELLS; i++) cancer.add(new CellPosition(i, 1));
    assertThrows(IOException.class, () -> codec.write(new PatternData(List.of(), cancer), out));
    assertArrayEquals(new byte[] {7}, out.toByteArray());
  }

  @Test
  void preservesJvmWideFilter() throws Exception {
    // Given: isolate JVM-wide state from the JUnit worker.
    String main =
        Path.of(LegacyLifeCodec.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .toString();
    String tests =
        Path.of(GlobalFilterProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .toString();
    var process =
        new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Djdk.serialFilter=!java.awt.Point",
                "-cp",
                main + File.pathSeparator + tests,
                GlobalFilterProbe.class.getName())
            .redirectErrorStream(true)
            .start();
    try {
      // When
      assertTrue(process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS));
      // Then
      assertEquals(0, process.exitValue(), new String(process.getInputStream().readAllBytes()));
    } finally {
      process.destroyForcibly();
    }
  }

  public static final class GlobalFilterProbe {
    public static void main(String[] arguments) throws Exception {
      var bytes = new ByteArrayOutputStream();
      new ObjectOutputStream(bytes).writeObject(new LinkedList<>(List.of(new Point(1, 2))));
      try {
        new LegacyLifeCodec().read(new ByteArrayInputStream(bytes.toByteArray()));
        throw new AssertionError("global filter was relaxed");
      } catch (InvalidClassException expected) {
      }
    }
  }

  @Test
  void leavesTrailingDataAndStreamOwnershipToCaller() throws Exception {
    var out = new ByteArrayOutputStream();
    out.write(serialized(new LinkedList<>(List.of(new Point(1, 2)))));
    out.write(new byte[MAX_BYTES + 1]);
    var input = new TrackedInput(out.toByteArray());
    var data = codec.read(input);
    assertEquals(List.of(new CellPosition(2, 1)), data.cells());
    assertTrue(input.available() >= MAX_BYTES);
    assertFalse(input.closed);
  }

  private static Object nested(int depth) {
    Object value = new Point(1, 2);
    for (int i = 1; i < depth; i++) value = new ArrayList<>(List.of(value));
    return value;
  }

  private static byte[] paddedEmptyList(int size) throws IOException {
    byte[] value = serialized(new LinkedList<Point>());
    byte[] bytes = new byte[size];
    System.arraycopy(value, 0, bytes, 0, 4);
    Arrays.fill(bytes, 4, size - value.length + 4, (byte) ObjectStreamConstants.TC_RESET);
    System.arraycopy(value, 4, bytes, size - value.length + 4, value.length - 4);
    return bytes;
  }

  private static byte[] serialized(Object value) throws IOException {
    var bytes = new ByteArrayOutputStream();
    try (var out = new ObjectOutputStream(bytes)) {
      out.writeObject(value);
    }
    return bytes.toByteArray();
  }

  private static final class TrackedInput extends ByteArrayInputStream {
    boolean closed;

    TrackedInput(byte[] bytes) {
      super(bytes);
    }

    int consumed() {
      return pos;
    }

    @Override
    public void close() {
      closed = true;
    }
  }
}
