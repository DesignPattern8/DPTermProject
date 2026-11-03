package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.holub.life.model.Cell;
import com.holub.life.model.Resident;
import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 클래식 모드 저장·불러오기(Memento) 특성 테스트 (docs/test-cases.md CL-M). */
class ClassicMementoTest {
  private static final int[][] BLINKER = {{10, 9}, {10, 10}, {10, 11}};
  private static final int[][] BLOCK = {{30, 30}, {30, 31}, {31, 30}, {31, 31}};
  private static final int[][] GLIDER = {{1, 2}, {2, 3}, {3, 1}, {3, 2}, {3, 3}};

  @Test
  @DisplayName("CL-M-01 저장한 보드를 불러오면 같은 칸이 살아 있다")
  void storeThenLoadRestoresSameCells() throws IOException {
    // Given
    ClassicBoard original = new ClassicBoard().clickAll(GLIDER).clickAll(BLOCK);
    byte[] file = original.store();
    // When
    ClassicBoard loaded = new ClassicBoard().load(file);
    // Then
    assertEquals(original.liveCells(), loaded.liveCells());
  }

  @Test
  @DisplayName("CL-M-02 불러오기는 기존 보드를 지우고 덮어쓴다")
  void loadReplacesExistingCells() throws IOException {
    // Given
    byte[] blockFile = new ClassicBoard().clickAll(BLOCK).store();
    ClassicBoard board = new ClassicBoard().clickAll(BLINKER);
    // When
    board.load(blockFile);
    // Then
    assertEquals(ClassicBoard.cells(BLOCK), board.liveCells());
  }

  @Test
  @DisplayName("CL-M-03 불러온 보드도 tick하면 규칙대로 진행한다")
  void loadedBoardEvolvesOnTick() throws IOException {
    // Given
    ClassicBoard board = new ClassicBoard().load(new ClassicBoard().clickAll(BLINKER).store());
    // When
    board.tick();
    // Then
    assertEquals(ClassicBoard.cells(new int[][] {{9, 10}, {10, 10}, {11, 10}}), board.liveCells());
  }

  @Test
  @DisplayName("CL-M-04 저장 형식은 Point(x=열, y=행)의 직렬화된 Collection이다")
  void storedFormatIsSerializedPointCollection() throws Exception {
    // Given
    ClassicBoard board = new ClassicBoard().click(5, 6);
    // When
    Collection<?> stored = ClassicBoard.readStoredCollection(board.store());
    // Then
    assertTrue(stored.contains(new Point(6, 5)));
    for (Object o : stored) assertInstanceOf(Point.class, o);
  }

  @Test
  @DisplayName("CL-M-05 빈 보드를 저장·불러오기 할 수 있다")
  void emptyBoardRoundTrip() throws IOException {
    // Given
    byte[] file = new ClassicBoard().store();
    // When
    ClassicBoard loaded = new ClassicBoard().load(file);
    // Then
    assertTrue(loaded.liveCells().isEmpty());
  }

  @Test
  @DisplayName("CL-M-06 [현재 동작] Store 경로는 살아 있는 칸을 두 번씩 기록한다")
  void storeRecordsEachLiveCellTwice() throws Exception {
    // Given
    ClassicBoard board = new ClassicBoard().click(5, 5).click(5, 6);
    // When
    Collection<?> stored = ClassicBoard.readStoredCollection(board.store());
    // Then
    List<Object> expected = new ArrayList<>(List.of(new Point(5, 5), new Point(6, 5)));
    expected.addAll(List.of(new Point(5, 5), new Point(6, 5)));
    assertEquals(expected, new ArrayList<>(stored));
  }

  @Test
  @DisplayName("CL-M-07 직렬화 형식이 아닌 파일을 불러오면 IOException이 난다")
  void loadingGarbageThrowsIOException() {
    // Given
    byte[] garbage = "garbage".getBytes();
    ClassicBoard board = new ClassicBoard();
    // When / Then
    assertThrows(IOException.class, () -> board.load(garbage));
  }

  @Test
  @DisplayName("CL-M-08 [현재 동작] Collection이 아닌 객체가 든 파일을 불러오면 IOException이 아니라 ClassCastException이 난다")
  void loadingNonCollectionThrowsClassCastException() throws IOException {
    // Given
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject("not a collection");
    }
    ClassicBoard board = new ClassicBoard();
    // When / Then
    assertThrows(ClassCastException.class, () -> board.load(bytes.toByteArray()));
  }

  @Test
  @DisplayName("CL-M-09 단일 셀과 DUMMY는 Memento를 만들 수 없다")
  void unitaryCellsCannotCreateMemento() {
    // When / Then
    assertThrows(UnsupportedOperationException.class, () -> new Resident().createMemento());
    assertThrows(UnsupportedOperationException.class, () -> Cell.DUMMY.createMemento());
  }
}
