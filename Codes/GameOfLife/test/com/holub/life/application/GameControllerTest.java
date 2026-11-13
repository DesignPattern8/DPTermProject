package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import java.awt.Point;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class GameControllerTest {
  @TempDir Path directory;
  private Timer timer;
  private Cell board;
  private GameController controller;

  @BeforeEach
  void setUp() {
    timer = new Timer(true);
    Neighborhood root = new Neighborhood(8, new Neighborhood(8, new Resident()));
    board = root;
    controller =
        new GameController(
            new CompositeBoard(root), root, new Clock(new UtilTimerTickSource(timer)));
  }

  @AfterEach
  void cleanUp() {
    timer.cancel();
  }

  @Test
  void tickAdvancesOneGeneration() {
    board.toggle(10, 9);
    board.toggle(10, 10);
    board.toggle(10, 11);
    controller.tick();
    assertEquals(
        Set.of(new Point(10, 9), new Point(10, 10), new Point(10, 11)),
        new HashSet<>(board.createMemento().liveCells()));
  }

  @Test
  void missingFileKeepsBoard() {
    board.toggle(5, 5);
    assertThrows(
        FileNotFoundException.class,
        () -> controller.load(directory.resolve("missing.life").toFile()));
    assertTrue(board.createMemento().isAlive(new Point(5, 5)));
  }

  @Test
  void invalidFileClearsBoard() throws Exception {
    board.toggle(5, 5);
    Path file = directory.resolve("bad.life");
    Files.write(file, new byte[] {1, 2, 3, 4});
    assertThrows(IOException.class, () -> controller.load(file.toFile()));
    assertTrue(board.createMemento().liveCells().isEmpty());
  }

  @Test
  void storeWritesEachCoordinateOnceAndRoundTrip() throws Exception {
    board.toggle(5, 6);
    Path file = directory.resolve("board.life");
    controller.store(file.toFile());
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
      assertEquals(List.of(new Point(6, 5)), in.readObject());
    }
    controller.clear();
    controller.load(file.toFile());
    assertEquals(List.of(new Point(6, 5)), board.createMemento().liveCells());
  }

  @Test
  void invalidOutputDoesNotClearBoard() {
    board.toggle(5, 5);
    assertThrows(IOException.class, () -> controller.store(directory.toFile()));
    assertTrue(board.createMemento().isAlive(new Point(5, 5)));
  }

  @Test
  @DisplayName("APP-S01 정상 Load 후 입력 스트림을 닫는다")
  void successfulLoadReleasesFile() throws Exception {
    board.toggle(5, 6);
    Path file = directory.resolve("board.life");
    controller.store(file.toFile());
    controller.clear();
    long before = openFileCount();

    controller.load(file.toFile());

    assertEquals(List.of(new Point(6, 5)), board.createMemento().liveCells());
    assertFileReleased(file, before);
  }

  @Test
  @DisplayName("APP-S02 형식이 잘못된 파일 Load가 실패해도 입력 스트림을 닫는다")
  void invalidFileLoadReleasesFile() throws Exception {
    Path file = directory.resolve("bad.life");
    Files.write(file, new byte[] {1, 2, 3, 4});
    long before = openFileCount();

    assertThrows(IOException.class, () -> controller.load(file.toFile()));

    assertFileReleased(file, before);
  }

  @Test
  @DisplayName("APP-S03 빈 파일 Load가 실패해도 입력 스트림을 닫는다")
  void emptyFileLoadReleasesFile() throws Exception {
    Path file = directory.resolve("empty.life");
    Files.write(file, new byte[0]);
    long before = openFileCount();

    assertThrows(IOException.class, () -> controller.load(file.toFile()));

    assertFileReleased(file, before);
  }

  @Test
  @DisplayName("APP-S04 정상 Store 후 출력 스트림을 닫는다")
  void successfulStoreReleasesFile() throws Exception {
    board.toggle(5, 6);
    Path file = directory.resolve("board.life");
    long before = openFileCount();

    controller.store(file.toFile());

    assertFileReleased(file, before);
  }

  private List<GameListener.Change> recordChanges() {
    List<GameListener.Change> changes = new ArrayList<>();
    controller.addGameListener(changes::add);
    return changes;
  }

  @Test
  @DisplayName("APP-R04 셀 편집 후 전체 갱신을 알린다")
  void toggleNotifiesEdited() {
    List<GameListener.Change> changes = recordChanges();

    controller.toggle(5, 5);

    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  @DisplayName("APP-R05 Clear 후 전체 갱신을 알린다")
  void clearNotifiesEdited() {
    board.toggle(5, 5);
    List<GameListener.Change> changes = recordChanges();

    controller.clear();

    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  @DisplayName("APP-R06 세대가 바뀐 tick 후 부분 갱신을 알린다")
  void changingTickNotifiesGeneration() {
    board.toggle(10, 9);
    board.toggle(10, 10);
    board.toggle(10, 11);
    List<GameListener.Change> changes = recordChanges();

    assertTrue(controller.tick());

    assertEquals(List.of(GameListener.Change.GENERATION), changes);
  }

  @Test
  @DisplayName("APP-R07 바뀐 칸이 없는 tick은 알리지 않는다")
  void unchangedTickDoesNotNotify() {
    List<GameListener.Change> changes = recordChanges();

    assertFalse(controller.tick());

    assertTrue(changes.isEmpty());
  }

  @Test
  @DisplayName("APP-R08 불러오기 성공 후 전체 갱신을 알린다")
  void loadNotifiesEdited() throws Exception {
    board.toggle(5, 6);
    Path file = directory.resolve("board.life");
    controller.store(file.toFile());
    List<GameListener.Change> changes = recordChanges();

    controller.load(file.toFile());

    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  @DisplayName("APP-R09 불러오기에 실패해도 전체 갱신을 알린다")
  void failedLoadStillNotifiesEdited() throws Exception {
    board.toggle(5, 5);
    Path file = directory.resolve("bad.life");
    Files.write(file, new byte[] {1, 2, 3, 4});
    List<GameListener.Change> changes = recordChanges();

    assertThrows(IOException.class, () -> controller.load(file.toFile()));

    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  @DisplayName("APP-R10 저장은 보드를 바꾸지 않으므로 알리지 않는다")
  void storeDoesNotNotify() throws Exception {
    board.toggle(5, 6);
    List<GameListener.Change> changes = recordChanges();

    controller.store(directory.resolve("board.life").toFile());

    assertTrue(changes.isEmpty());
  }

  @Test
  @DisplayName("APP-B06 범위 밖 편집은 보드를 바꾸지 않고 거부하며 알리지 않는다")
  void outOfRangeToggleIsRejected() {
    List<GameListener.Change> changes = recordChanges();

    assertThrows(IndexOutOfBoundsException.class, () -> controller.toggle(64, 0));

    assertTrue(board.createMemento().liveCells().isEmpty());
    assertTrue(changes.isEmpty());
  }

  /** Open file descriptors on Unix-like systems; -1 where the JVM cannot report it (Windows). */
  private static long openFileCount() {
    java.lang.management.OperatingSystemMXBean os =
        java.lang.management.ManagementFactory.getOperatingSystemMXBean();
    if (os instanceof com.sun.management.UnixOperatingSystemMXBean unix)
      return unix.getOpenFileDescriptorCount();
    return -1;
  }

  /**
   * Windows refuses to delete a file that is still open. Unix deletes it anyway, so there the open
   * descriptor count must not have grown.
   */
  private static void assertFileReleased(Path file, long openFilesBefore) throws IOException {
    if (openFilesBefore >= 0)
      assertTrue(
          openFileCount() <= openFilesBefore,
          "file descriptor left open: before=" + openFilesBefore + ", after=" + openFileCount());
    Files.delete(file);
  }
}
