package com.holub.life.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.Cell;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.NeighborhoodState;
import com.holub.life.model.Resident;
import java.awt.Point;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancerCodecTest {
  private static Neighborhood newBoard() {
    return new Neighborhood(8, new Neighborhood(8, new Resident()));
  }

  private static Cell cellAt(Neighborhood board, int row, int column) {
    Neighborhood block = (Neighborhood) board.childAt(row / 8, column / 8);
    return block.childAt(row % 8, column % 8);
  }

  private static byte[] store(Neighborhood board) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    LegacyLifeCodec.flush(board.createMemento(), out);
    return out.toByteArray();
  }

  private static Neighborhood load(byte[] bytes) throws IOException {
    Neighborhood board = newBoard();
    NeighborhoodState state = board.createMemento();
    LegacyLifeCodec.load(state, new ByteArrayInputStream(bytes));
    board.transfer(state, new Point(0, 0), Cell.LOAD);
    return board;
  }

  @Test
  @DisplayName("CAN-P01 암세포가 있는 보드를 저장했다가 불러오면 생존·암 위치가 그대로다")
  void cancerBoardRoundTrips() throws IOException {
    // Given
    Neighborhood board = newBoard();
    board.toggle(1, 1);
    cellAt(board, 2, 2).setCancer();
    cellAt(board, 40, 50).setCancer();

    // When
    Neighborhood loaded = load(store(board));

    // Then
    assertEquals(3, loaded.createMemento().liveCells().size());
    assertTrue(cellAt(loaded, 1, 1).isAlive());
    assertFalse(cellAt(loaded, 1, 1).isCancer());
    assertTrue(cellAt(loaded, 2, 2).isCancer());
    assertTrue(cellAt(loaded, 40, 50).isCancer());
    assertEquals(2, loaded.cancerCount());
  }

  @Test
  @DisplayName("CAN-P02 암세포가 없으면 기존 Collection<Point> 형식으로 저장한다")
  void boardWithoutCancerKeepsLegacyFormat() throws Exception {
    Neighborhood board = newBoard();
    board.toggle(1, 1);
    board.toggle(2, 3);

    Object stored =
        new ObjectInputStream(new ByteArrayInputStream(store(board))).readObject();

    assertInstanceOf(Collection.class, stored);
    assertEquals(Set.of(new Point(1, 1), new Point(3, 2)), new HashSet<>((Collection<?>) stored));
  }

  @Test
  @DisplayName("CAN-P03 기존 형식 파일은 암세포 없이 불러온다")
  void legacyFileLoadsWithoutCancer() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ObjectOutputStream sink = new ObjectOutputStream(out)) {
      sink.writeObject(new ArrayList<>(List.of(new Point(3, 4))));
    }

    Neighborhood loaded = load(out.toByteArray());

    assertTrue(cellAt(loaded, 4, 3).isAlive());
    assertEquals(0, loaded.cancerCount());
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  @DisplayName("CAN-P04 암세포 형식에 Point가 아닌 원소가 있으면 IOException이 난다")
  void cancerFileWithNonPointEntryIsRejected() throws IOException {
    NeighborhoodState state = new NeighborhoodState();
    state.replaceCells(
        (Collection) new ArrayList<>(List.of("not a point")),
        new HashSet<>(Set.of(new Point(1, 1))));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    LegacyLifeCodec.flush(state, out);

    assertThrows(
        IOException.class,
        () ->
            LegacyLifeCodec.load(
                new NeighborhoodState(), new ByteArrayInputStream(out.toByteArray())));
  }
}
