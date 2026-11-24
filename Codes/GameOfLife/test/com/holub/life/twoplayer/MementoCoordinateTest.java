package com.holub.life.twoplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.holub.life.application.PersistenceMapper;
import com.holub.life.model.Board;
import com.holub.life.model.BoardSnapshot;
import com.holub.life.model.CancerCell;
import com.holub.life.model.Cell;
import com.holub.life.model.CellPosition;
import com.holub.life.model.CompositeBoard;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.NeighborhoodState;
import com.holub.life.model.Resident;
import com.holub.life.persistence.LegacyLifeCodec;
import com.holub.life.persistence.PatternData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MementoCoordinateTest {
  @ParameterizedTest(name = "depth={0}")
  @ValueSource(ints = {2, 3})
  @DisplayName("COORD-01 STORE는 비영원점과 2·3중 블록 순회 순서를 보존한다")
  void storeKeepsNestedBlockOrderAndOffset(int depth) {
    // Given
    Neighborhood root = neighborhood(depth);
    int size = root.widthInCells();
    int half = size / 2;
    root.toggle(0, half);
    root.toggle(1, 0);
    root.toggle(half, 1);
    root.toggle(size - 1, size - 1);
    cellAt(root, 1, 0).setCancer();
    NeighborhoodState state = new NeighborhoodState();
    var origin = position(11, 23);

    // When
    root.transfer(state, origin, Cell.STORE);

    // Then
    assertEquals(
        new PatternData(
            List.of(
                new CellPosition(12, 23),
                new CellPosition(11, 23 + half),
                new CellPosition(11 + half, 24),
                new CellPosition(10 + size, 22 + size)),
            Set.of(new CellPosition(12, 23))),
        PersistenceMapper.capture(state));
    assertEquals(position(11, 23), origin);
  }

  @Test
  @DisplayName("COORD-02 LOAD는 비영원점 영역만 읽고 이전 생사·암 상태를 교체한다")
  void loadUsesOffsetAndReplacesLiveAndCancerState() {
    // Given
    Neighborhood root = neighborhood(3);
    Board board = new CompositeBoard(root);
    root.toggle(1, 4);
    cellAt(root, 1, 4).setCancer();
    root.toggle(6, 2);
    root.toggle(3, 3);
    cellAt(root, 3, 3).setCancer();
    root.toggle(0, 7);
    NeighborhoodState state = new NeighborhoodState();
    state.markAsAlive(position(12, 27));
    state.markAsCancer(position(17, 25));
    state.markAsAlive(position(10, 24));
    state.markAsCancer(position(19, 24));
    state.markAsAlive(position(12, 22));
    state.markAsCancer(position(12, 31));
    PatternData beforeLoad = PersistenceMapper.capture(state);

    // When
    assertTrue(root.transfer(state, position(11, 23), Cell.LOAD));

    // Then
    assertEquals(
        new BoardSnapshot(
            8,
            8,
            Set.of(new CellPosition(1, 4), new CellPosition(6, 2)),
            Set.of(new CellPosition(6, 2))),
        board.snapshot());
    for (CellPosition changed :
        List.of(
            new CellPosition(1, 4),
            new CellPosition(6, 2),
            new CellPosition(3, 3),
            new CellPosition(0, 7))) {
      assertFalse(cellAt(root, changed.row(), changed.column()).transition());
    }
    assertEquals(beforeLoad, PersistenceMapper.capture(state));
  }

  @Test
  @DisplayName("COORD-03 State는 삽입 순서와 중복을 유지하고 암 좌표도 생존 목록에 기록한다")
  void stateKeepsInsertionOrderDuplicatesAndCancerSubset() {
    // Given
    NeighborhoodState state = new NeighborhoodState();

    // When
    state.markAsAlive(position(2, 5));
    state.markAsCancer(position(7, 1));
    state.markAsAlive(position(2, 5));
    state.markAsCancer(position(7, 1));

    // Then
    PatternData captured = PersistenceMapper.capture(state);
    assertEquals(
        List.of(
            new CellPosition(2, 5),
            new CellPosition(7, 1),
            new CellPosition(2, 5),
            new CellPosition(7, 1)),
        captured.cells());
    assertEquals(Set.of(new CellPosition(7, 1)), captured.cancerCells());
    assertTrue(captured.cells().containsAll(captured.cancerCells()));
    assertTrue(state.isAlive(position(2, 5)));
    assertFalse(state.isCancer(position(2, 5)));
    assertTrue(state.isAlive(position(7, 1)));
    assertTrue(state.isCancer(position(7, 1)));
  }

  @Test
  @DisplayName("COORD-04 replaceCells는 가변 컬렉션 참조를 유지하고 단일 인자는 암 집합을 초기화한다")
  void replacementKeepsMutableAliasesAndResetsCancer() {
    // Given
    var live = new ArrayList<>(List.of(position(2, 5), position(7, 1)));
    var cancer = new HashSet<>(Set.of(position(7, 1)));
    NeighborhoodState state = new NeighborhoodState();
    state.replaceCells(live, cancer);
    assertSame(live, state.liveCells());
    assertSame(cancer, state.cancerCells());

    // When / Then
    live.add(position(4, 6));
    assertTrue(state.isAlive(position(4, 6)));
    state.liveCells().remove(position(2, 5));
    assertFalse(live.contains(position(2, 5)));
    state.cancerCells().clear();
    assertTrue(cancer.isEmpty());
    cancer.add(position(7, 1));
    assertTrue(state.isCancer(position(7, 1)));

    var replacement = new LinkedList<>(List.of(position(8, 3), position(8, 3)));
    state.replaceCells(replacement);

    assertSame(replacement, state.liveCells());
    assertTrue(state.cancerCells().isEmpty());
    assertEquals(Set.of(position(7, 1)), cancer);
    assertEquals(
        new PatternData(List.of(new CellPosition(8, 3), new CellPosition(8, 3)), Set.of()),
        PersistenceMapper.capture(state));
  }

  @Test
  @DisplayName("COORD-05 DUMMY와 2인용 셀은 transfer를 무시하고 단위 셀 Memento 생성은 거부한다")
  void unsupportedCellsKeepTransferNoOpAndMementoFailure() {
    // Given
    TwoPlayerResident twoPlayer = new TwoPlayerResident();
    twoPlayer.prepareOwner(Player.A);
    twoPlayer.toggle(0, 0);
    NeighborhoodState state = new NeighborhoodState();
    state.markAsCancer(position(2, 5));
    PatternData beforeTransfer = PersistenceMapper.capture(state);

    // When / Then
    for (boolean load : new boolean[] {Cell.STORE, Cell.LOAD}) {
      assertFalse(Cell.DUMMY.transfer(state, position(11, 23), load));
      assertFalse(twoPlayer.transfer(state, position(11, 23), load));
    }
    assertEquals(beforeTransfer, PersistenceMapper.capture(state));
    assertFalse(Cell.DUMMY.isAlive());
    assertFalse(Cell.DUMMY instanceof CancerCell);
    assertEquals(Player.A, twoPlayer.owner());
    assertThrows(UnsupportedOperationException.class, () -> new Resident().createMemento());
    assertThrows(UnsupportedOperationException.class, () -> Cell.DUMMY.createMemento());
    assertThrows(UnsupportedOperationException.class, twoPlayer::createMemento);
  }

  @Test
  @DisplayName("COORD-06 null 좌표 기록은 빈 State를 바꾸지 않고 실패한다")
  void nullMarksFailWithoutAddingCells() {
    // Given
    NeighborhoodState state = new NeighborhoodState();

    // When / Then
    assertThrows(NullPointerException.class, () -> state.markAsAlive(null));
    assertThrows(NullPointerException.class, () -> state.markAsCancer(null));
    assertTrue(state.liveCells().isEmpty());
    assertTrue(state.cancerCells().isEmpty());
  }

  @Test
  @DisplayName("COORD-07 모델·파일 왕복은 음수 행·열과 중복·암 좌표를 보존한다")
  void modelFileRoundTripKeepsNegativeDuplicateAndCancerCoordinates() throws IOException {
    // Given
    NeighborhoodState state = new NeighborhoodState();
    state.markAsAlive(position(-2, 9));
    state.markAsCancer(position(6, -4));
    state.markAsAlive(position(-2, 9));
    state.markAsCancer(position(6, -4));
    PatternData expected =
        new PatternData(
            List.of(
                new CellPosition(-2, 9),
                new CellPosition(6, -4),
                new CellPosition(-2, 9),
                new CellPosition(6, -4)),
            Set.of(new CellPosition(6, -4)));
    LegacyLifeCodec codec = new LegacyLifeCodec();
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    // When
    codec.write(PersistenceMapper.capture(state), bytes);
    PatternData decoded = codec.read(new ByteArrayInputStream(bytes.toByteArray()));
    NeighborhoodState restored = PersistenceMapper.restore(decoded);

    // Then
    assertEquals(expected, decoded);
    assertEquals(expected, PersistenceMapper.capture(restored));
  }

  private static CellPosition position(int row, int column) {
    return new CellPosition(row, column);
  }

  private static Neighborhood neighborhood(int depth) {
    Cell cell = new Resident();
    for (int level = 0; level < depth; level++) cell = new Neighborhood(2, cell);
    return (Neighborhood) cell;
  }

  private static Resident cellAt(Neighborhood root, int row, int column) {
    Cell cell = root;
    while (cell instanceof Neighborhood block) {
      int childWidth = block.childAt(0, 0).widthInCells();
      cell = block.childAt(row / childWidth, column / childWidth);
      row %= childWidth;
      column %= childWidth;
    }
    return (Resident) cell;
  }
}
