# D가 호출하는 A·B 인터페이스 (요청안)

- 제공 도메인: A(`com.holub.life.model`), B(`com.holub.life.application`) / 사용 도메인: D(`com.holub.life.ui`) / 작성: 병국
- 이 문서는 **D가 필요로 하는 것을 요청**하는 문서다. 최종 시그니처는 제공 도메인이 정하고, 합의되면 D 코드의 이름만 맞춘다.
- 변경 이력
  - 11/3 초안 작성, A·B 검토 요청

## A에게 요청: 좌표와 스냅샷

```java
package com.holub.life.model;

public final class CellPosition {        // 값 객체 (record여도 됨)
  public CellPosition(int row, int column);
  public int row();
  public int column();
}

public interface BoardSnapshot {         // 변경 불가능
  int rows();
  int columns();
  boolean isAlive(CellPosition position);

  // 기존 화면 표시(블록 테두리, 활성 블록 파란 테두리)를 유지하기 위해 필요
  int blockSize();                                   // 한 블록의 한 변 셀 수 (현재 8)
  boolean isBlockActive(int blockRow, int blockColumn); // 마지막 세대에서 바뀐 블록인가
  boolean isBoardActive();                           // 보드 전체에 바뀐 셀이 있는가
}
```

**계약**
- 스냅샷은 만든 뒤 바뀌지 않는다. D는 다른 스레드에서 받은 스냅샷을 그대로 그린다.
- `rows()`, `columns()`는 `blockSize()`의 배수다.
- `isBlockActive` / `isBoardActive`는 기존 `Neighborhood.amActive`와 같은 의미다(파란 테두리 표시용). 기존 화면에 있던 기능이라 빠지면 표시가 달라진다. 2단계로 구성된 지금 구조(보드 → 블록 → 셀)를 기준으로 했다.

## B에게 요청: 컨트롤러, 알림, 타이머

```java
package com.holub.life.application;

public interface GameController {
  BoardSnapshot snapshot();               // 첫 화면 그리기용 현재 상태
  void toggleCell(CellPosition position);
  void clear();
  void step();                            // 한 세대 진행 (Tick)
  void start(int intervalMillis);         // 실행 또는 속도 변경
  void stop();                            // Halt
  void load(File file) throws IOException;
  void store(File file) throws IOException;
  void addListener(GameListener listener);
}

public interface GameListener {
  void boardChanged(BoardSnapshot snapshot);   // 보드가 바뀐 뒤 호출
}

public interface TickSource {             // D가 SwingTickSource로 구현
  void start(int intervalMillis, Runnable onTick);
  void stop();
}
```

**계약**
- 모든 보드 변경(`toggleCell`, `clear`, `step`, 타이머 진행, `load`) 뒤에 `boardChanged`를 호출한다. 기존에는 `Universe`가 클릭·Clear·Load 뒤에 `repaint()`를 직접 불렀다. 새 구조에서는 이 알림이 그 역할을 한다.
- `load` / `store`는 기존 `Universe.doLoad/doStore`처럼 **시작하기 전에 실행을 멈춘다**(`Clock.instance().stop()`). 실패하면 `IOException`을 던진다. D는 이 예외를 받아 `"Read Failed!"` / `"Write Failed!"`를 표시한다.
  - 대안: 결과 객체(성공/실패)로 돌려주기. 예외 방식은 기존 코드와 같고 D 쪽 코드가 단순해서 먼저 제안한다.
- `start(ms)`는 이미 실행 중이면 속도만 바꾼다(기존 `startTicking`과 같음). `start(0)` 같은 값으로 정지를 표현하지 않고 `stop()`을 쓴다.
- `boardChanged`는 어느 스레드에서 불러도 된다. D는 `repaint()`만 하므로 스레드 제약이 없다.

## D가 제공하는 것

| 타입 | 용도 |
| --- | --- |
| `SwingTickSource implements TickSource` | `javax.swing.Timer` 기반. 틱이 Swing 이벤트 스레드에서 실행된다 ([004](004-swing-tick-source.md)) |
| `BoardPanel implements GameListener` | 컨트롤러에 자신을 등록한다 |
| 조립 (`Life.java`) | 2단계에서 A·B·C 구현을 생성하고 연결한다. 각 담당자가 생성 방법을 알려 준다 |
