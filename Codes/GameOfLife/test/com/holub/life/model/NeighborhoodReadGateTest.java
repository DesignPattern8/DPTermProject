package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NeighborhoodReadGateTest {
  private static final int TIMEOUT_SECONDS = 5;

  @ParameterizedTest(name = "depth={0}")
  @ValueSource(ints = {2, 3})
  @DisplayName("GATE-01 전이 중 같은 트리의 루트와 모든 중첩 블록은 읽기를 허용하지 않는다")
  void sameBoardBlocksShareReadGate(int depth) {
    // Given: 한 루트 아래에 2중 또는 3중 블록을 둔다.
    AtomicReference<Runnable> callback = new AtomicReference<>();
    Neighborhood root = board(depth, callback);
    List<List<Boolean>> observed = new ArrayList<>();
    callback.set(() -> observed.add(readGates(root)));

    // When: 첫 leaf가 전이하는 동안 모든 블록의 읽기 허가를 수집한다.
    root.transition();

    // Then: 전이 중에는 모두 닫히고, 전체 전이가 끝나면 모두 열린다.
    assertReadings(observed, false);
    assertGateStates(readGates(root), true);
  }

  @ParameterizedTest(name = "factory={0}")
  @ValueSource(strings = {"independent", "sharedPrototype", "publicCreate"})
  @DisplayName("GATE-02 별도 생성·공유 프로토타입·공개 create로 만든 보드는 읽기 허가가 독립적이다")
  void independentBoardsKeepReadGatesSeparate(String factory) {
    // Given: 프로토타입 원본을 보관하고, 두 개의 루트를 만든다.
    AtomicReference<Runnable> callback = new AtomicReference<>();
    Neighborhood prototype = board(1, callback);
    Neighborhood first = new Neighborhood(2, prototype);
    Neighborhood second =
        switch (factory) {
          case "independent" -> board(2, new AtomicReference<>());
          case "sharedPrototype" -> new Neighborhood(2, prototype);
          case "publicCreate" -> (Neighborhood) first.create();
          default -> throw new IllegalArgumentException(factory);
        };
    List<List<Boolean>> observed = new ArrayList<>();
    callback.set(
        () -> {
          observed.add(readGates(first));
          observed.add(readGates(second));
          observed.add(readGates(prototype));
        });

    // When: 첫 보드만 전이한다.
    first.transition();

    // Then: 같은 트리는 닫히지만 다른 보드와 프로토타입 원본은 계속 열린다.
    assertReadings(observed, false, true, true);
    assertGateStates(readGates(first), true);
    assertGateStates(readGates(second), true);
    assertGateStates(readGates(prototype), true);
  }

  @Test
  @DisplayName("GATE-03 다른 보드의 중첩 전이가 먼저 끝나면 그 보드만 읽기를 허용한다")
  void nestedBoardTransitionsKeepGatesIndependent() {
    // Given: 첫 보드의 leaf 전이 안에서 둘째 보드의 전이를 호출한다.
    AtomicReference<Runnable> firstCallback = new AtomicReference<>();
    AtomicReference<Runnable> secondCallback = new AtomicReference<>();
    Neighborhood first = board(2, firstCallback);
    Neighborhood second = board(2, secondCallback);
    List<List<Boolean>> observed = new ArrayList<>();
    secondCallback.set(
        () -> {
          observed.add(readGates(first));
          observed.add(readGates(second));
        });
    firstCallback.set(
        () -> {
          observed.add(readGates(first));
          observed.add(readGates(second));
          second.transition();
          observed.add(readGates(first));
          observed.add(readGates(second));
        });

    // When: 첫 보드의 전이가 둘째 보드의 전이를 감싼다.
    first.transition();

    // Then: 둘째 보드 진입 전·전이 중·종료 후 각각 독립된 허가를 유지한다.
    assertReadings(observed, false, true, false, false, false, true);
    assertGateStates(readGates(first), true);
    assertGateStates(readGates(second), true);
  }

  @Test
  @DisplayName("GATE-04 두 스레드에서 겹친 전이 중 먼저 끝난 보드만 읽기를 허용한다")
  void concurrentBoardTransitionsFinishIndependently() throws Exception {
    // Given: 각 보드의 leaf 전이를 별도의 래치에서 멈춘다.
    AtomicReference<Runnable> firstCallback = new AtomicReference<>();
    AtomicReference<Runnable> secondCallback = new AtomicReference<>();
    Neighborhood first = board(2, firstCallback);
    Neighborhood second = board(2, secondCallback);
    CountDownLatch firstEntered = new CountDownLatch(1);
    CountDownLatch secondEntered = new CountDownLatch(1);
    CountDownLatch releaseFirst = new CountDownLatch(1);
    CountDownLatch releaseSecond = new CountDownLatch(1);
    firstCallback.set(
        () -> {
          firstEntered.countDown();
          await(releaseFirst);
        });
    secondCallback.set(
        () -> {
          secondEntered.countDown();
          await(releaseSecond);
        });
    List<List<Boolean>> observed = new ArrayList<>();
    ExecutorService workers = Executors.newFixedThreadPool(2);

    // When: 두 전이를 실제로 겹치게 하고, 첫 보드만 먼저 종료시킨다.
    try {
      Future<Boolean> firstTransition = workers.submit(first::transition);
      await(firstEntered);
      Future<Boolean> secondTransition = workers.submit(second::transition);
      await(secondEntered);
      observed.add(readGates(first));
      observed.add(readGates(second));

      releaseFirst.countDown();
      firstTransition.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      observed.add(readGates(first));
      observed.add(readGates(second));

      releaseSecond.countDown();
      secondTransition.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } finally {
      releaseFirst.countDown();
      releaseSecond.countDown();
      workers.shutdownNow();
      if (!workers.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        throw new IllegalStateException("보드 전이 스레드가 종료되지 않았습니다");
      }
    }

    // Then: 함께 전이할 때는 둘 다 닫히고, 먼저 끝난 첫 보드는 독립적으로 열린다.
    assertReadings(observed, false, false, true, false);
    assertGateStates(readGates(first), true);
    assertGateStates(readGates(second), true);
  }

  @Test
  @DisplayName("GATE-05 같은 트리의 다른 하위 블록 전이가 끝나도 루트의 읽기 허가는 닫혀 있다")
  void nestedSubtreeTransitionKeepsOuterGateClosed() {
    // Given: 첫 하위 블록의 leaf 전이에서 다른 하위 블록을 다시 전이한다.
    AtomicReference<Runnable> callback = new AtomicReference<>();
    Neighborhood root = board(2, callback);
    Neighborhood sibling = (Neighborhood) root.childAt(1, 1);
    List<List<Boolean>> observed = new ArrayList<>();
    callback.set(
        () -> {
          observed.add(readGates(root));
          sibling.transition();
          observed.add(readGates(root));
        });

    // When: 하위 호출을 포함한 루트 전이를 실행한다.
    root.transition();

    // Then: 하위 호출 전후 모두 닫혀 있고 루트가 끝난 뒤에만 모두 열린다.
    assertReadings(observed, false, false);
    assertGateStates(readGates(root), true);
  }

  @ParameterizedTest(name = "error={0}")
  @ValueSource(booleans = {false, true})
  @DisplayName("GATE-06 중첩 leaf의 예외나 Error가 전파되어도 전체 허가와 다음 전이가 복구된다")
  void transitionFailureReleasesWholeBoard(boolean error) {
    // Given: 3중 블록의 leaf에서 지정한 오류 객체를 한 번만 던진다.
    AtomicReference<Runnable> callback = new AtomicReference<>();
    Neighborhood root = board(3, callback);
    Throwable expected =
        error ? new AssertionError("leaf 전이 실패") : new RuntimeException("leaf 전이 실패");
    callback.set(
        () -> {
          if (expected instanceof Error failure) throw failure;
          throw (RuntimeException) expected;
        });

    // When: 실패한 전이를 빠져나온 뒤, 같은 보드에서 다음 전이도 실행한다.
    Throwable actual =
        error
            ? assertThrows(AssertionError.class, root::transition)
            : assertThrows(RuntimeException.class, root::transition);
    List<Boolean> afterFailure = readGates(root);
    List<List<Boolean>> nextTransition = new ArrayList<>();
    callback.set(() -> nextTransition.add(readGates(root)));
    root.transition();

    // Then: 원래 오류를 그대로 전파하고, 허가와 중첩 깊이를 함께 복구한다.
    assertSame(expected, actual);
    assertGateStates(afterFailure, true);
    assertReadings(nextTransition, false);
    assertGateStates(readGates(root), true);
  }

  private static Neighborhood board(int depth, AtomicReference<Runnable> callback) {
    Cell cell = new TransitionCell(callback);
    for (int level = 0; level < depth; level++) cell = new Neighborhood(2, cell);
    return (Neighborhood) cell;
  }

  private static List<Boolean> readGates(Neighborhood root) {
    List<Boolean> gates = new ArrayList<>();
    collectReadGates(root, gates);
    return gates;
  }

  private static void collectReadGates(Neighborhood block, List<Boolean> gates) {
    try {
      gates.add(block.beginRedraw(true));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("읽기 허가 관찰이 중단되었습니다", e);
    }
    for (int row = 0; row < block.gridSize(); row++) {
      for (int column = 0; column < block.gridSize(); column++) {
        if (block.childAt(row, column) instanceof Neighborhood child) {
          collectReadGates(child, gates);
        }
      }
    }
  }

  private static void assertReadings(List<List<Boolean>> observed, boolean... expected) {
    assertEquals(expected.length, observed.size(), "전이 중 관찰 횟수");
    for (int index = 0; index < expected.length; index++) {
      assertGateStates(observed.get(index), expected[index]);
    }
  }

  private static void assertGateStates(List<Boolean> gates, boolean expected) {
    assertFalse(gates.isEmpty(), "루트의 읽기 허가도 관찰해야 합니다");
    for (int index = 0; index < gates.size(); index++) {
      assertEquals(expected, gates.get(index), "블록 " + index + "의 읽기 허가");
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        throw new IllegalStateException("보드 전이 래치 대기 시간이 초과되었습니다");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("보드 전이 래치 대기가 중단되었습니다", e);
    }
  }

  /** 생명 계산 없이 전이 시점만 관찰하며, 복제된 leaf들은 일회성 콜백을 공유한다. */
  private static final class TransitionCell implements Cell {
    private final AtomicReference<Runnable> callback;

    private TransitionCell(AtomicReference<Runnable> callback) {
      this.callback = callback;
    }

    @Override
    public boolean figureNextState(
        Cell north,
        Cell south,
        Cell east,
        Cell west,
        Cell northeast,
        Cell northwest,
        Cell southeast,
        Cell southwest) {
      return false;
    }

    @Override
    public Cell edge(int row, int column) {
      return this;
    }

    @Override
    public boolean transition() {
      Runnable once = callback.getAndSet(null);
      if (once != null) once.run();
      return false;
    }

    @Override
    public void toggle(int row, int column) {}

    @Override
    public boolean isAlive() {
      return false;
    }

    @Override
    public int widthInCells() {
      return 1;
    }

    @Override
    public Cell create() {
      return new TransitionCell(callback);
    }

    @Override
    public Direction isDisruptiveTo() {
      return Direction.NONE;
    }

    @Override
    public void clear() {}

    @Override
    public boolean transfer(Memento memento, Point upperLeftCorner, boolean doLoad) {
      return false;
    }

    @Override
    public NeighborhoodState createMemento() {
      throw new UnsupportedOperationException("전이 테스트 대역은 memento를 지원하지 않습니다");
    }
  }
}
