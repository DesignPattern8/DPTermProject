package com.holub.life;

import com.holub.life.model.Cell;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.NeighborhoodState;
import com.holub.life.model.Resident;
import com.holub.life.persistence.LegacyLifeCodec;
import com.holub.life.ui.GridCoordinateMapper;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 테스트용 클래식 보드. 원본 Universe(e38ce7a)와 같은 셀 구성을 Swing 없이 만들고, Universe의 메뉴·마우스·Clock 리스너가 모델에 하는 호출을
 * 그대로 재현한다. 좌표는 (행, 열)이고, 반환하는 {@link Point}는 원본 Memento 형식과 같이 x = 열, y = 행이다.
 */
final class ClassicBoard {
  static final int SIZE = 64;

  private final Cell outermost =
      new Neighborhood(8, new Neighborhood(8, new Resident())); // Universe.java:42-43

  Cell cell() {
    return outermost;
  }

  /** 마우스 클릭 (Universe.java:79). 픽셀 1개 = 셀 1개인 영역으로 클릭한다. */
  ClassicBoard click(int row, int column) {
    GridCoordinateMapper.userClicked(
        outermost, new Point(column, row), new Rectangle(0, 0, SIZE, SIZE));
    return this;
  }

  ClassicBoard clickAll(int[][] cells) {
    for (int[] c : cells) click(c[0], c[1]);
    return this;
  }

  /** Clock 리스너의 tick (Universe.java:127-141). */
  ClassicBoard tick() {
    Cell d = Cell.DUMMY;
    if (outermost.figureNextState(d, d, d, d, d, d, d, d)) outermost.transition();
    return this;
  }

  ClassicBoard tick(int times) {
    for (int i = 0; i < times; i++) tick();
    return this;
  }

  /** Grid &gt; Clear (Universe.java:90). */
  ClassicBoard clear() {
    outermost.clear();
    return this;
  }

  /** Grid &gt; Store (Universe.java:180-182). 파일 대신 바이트 배열에 쓴다. */
  byte[] store() throws IOException {
    NeighborhoodState memento = outermost.createMemento();
    outermost.transfer(memento, new Point(0, 0), Cell.STORE);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    LegacyLifeCodec.flush(memento, out);
    return out.toByteArray();
  }

  /** Grid &gt; Load (Universe.java:159-163). 파일 대신 바이트 배열에서 읽는다. */
  ClassicBoard load(byte[] bytes) throws IOException {
    outermost.clear();
    NeighborhoodState memento = outermost.createMemento();
    LegacyLifeCodec.load(memento, new ByteArrayInputStream(bytes));
    outermost.transfer(memento, new Point(0, 0), Cell.LOAD);
    return this;
  }

  /** 현재 살아 있는 칸. 셀 상태를 읽는 공개 API가 없어서 Memento를 메모리에서 직렬화해 읽는다. */
  Set<Point> liveCells() {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      LegacyLifeCodec.flush(outermost.createMemento(), out);
      Set<Point> live = new HashSet<>();
      for (Object o : readStoredCollection(out.toByteArray())) live.add((Point) o);
      return live;
    } catch (IOException | ClassNotFoundException e) {
      throw new AssertionError("Memento를 읽지 못함", e);
    }
  }

  static Collection<?> readStoredCollection(byte[] bytes)
      throws IOException, ClassNotFoundException {
    return (Collection<?>) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
  }

  /** (행, 열) 목록을 원본 Memento 형식의 Point 집합으로 바꾼다. */
  static Set<Point> cells(int[][] rowColumns) {
    Set<Point> s = new HashSet<>();
    for (int[] c : rowColumns) s.add(new Point(c[1], c[0]));
    return s;
  }

  static Set<Point> shifted(Set<Point> cells, int rows, int columns) {
    Set<Point> s = new HashSet<>();
    for (Point p : cells) s.add(new Point(p.x + columns, p.y + rows));
    return s;
  }
}
