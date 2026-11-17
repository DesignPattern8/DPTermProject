# 모델 계약

- 제공 A / 사용 B·C·D / 변경 2026-11-03, #24
- 기존 Cell/Resident/Neighborhood/Direction의 패키지는 com.holub.life.model이다.

```java
void Cell.toggle(int row, int column)
NeighborhoodState Cell.createMemento()
boolean Cell.transfer(Cell.Memento state, Point upperLeftCorner, boolean load)
int Neighborhood.gridSize()
Cell Neighborhood.childAt(int row, int column)
boolean Neighborhood.isActive()
boolean Neighborhood.beginRedraw(boolean drawAll) throws InterruptedException
```

좌표 순서는 행·열이다. toggle은 기존 클릭처럼 amAlive와 상위 블록 활성 상태를 바꾼다. 경계 밖 좌표를 보정하지 않는다. Point는 x=열/y=행을 유지한다. Memento는 기존 가변 컬렉션이며 중복·순서를 보존한다. createMemento는 내부에서 transfer(STORE)를 호출한다. transfer의 활성 정보 부수효과도 유지한다. Resident와 DUMMY의 createMemento는 UnsupportedOperationException이다.

beginRedraw는 기존 마지막 갱신 플래그를 소비하고 ConditionVariable 읽기 조건을 확인한다. 읽기 접근은 기존 동시성 계약을 유지하며 새 스레드 안전성을 보장하지 않는다. UI는 childAt으로 받은 셀을 읽기·좌표 변환에만 사용한다.

## 클래식 보드 계약

- 변경: 2026-11-10, #47 계약 문서 → #49 모델 API 구현. [설계 결정](../../../../../../../../docs/GTY.md#002-클래식-보드-facade)
- 변경: 2026-11-17, [#73](https://github.com/DesignPattern8/DPTermProject/issues/73)에서 암세포 상태 추가. [설계·검증 기록](../../../../../../../../docs/GTY.md#004-암세포-스냅샷-보존-73)
- 제공: A 모델(태영). 사용: B 실행·편집, C 저장·복원, D 화면.
- 생성: `CompositeBoard(Neighborhood neighborhood)`에 `Resident`로 구성한 클래식·암세포 정사각형 보드의 루트를 전달한다. 이 객체를 공유하는 호출은 직렬화한다.

| 사용처 | 현재 호출과 연결 기준 |
| --- | --- |
| B [GameController](../../application/GameController.java) | `Board.toggle`·`clear`·`step()`을 잠금 안에서 호출한다. 캡처·복원도 같은 잠금 안에서 직렬화해야 한다. |
| C [LegacyLifeCodec](../../persistence/LegacyLifeCodec.java) | 가변 `NeighborhoodState`와 기존 `.life` 파일을 주고받는다. 스냅샷 연결 시 파일 경계에서 좌표를 변환한다. |
| D [GridCoordinateMapper](../../ui/GridCoordinateMapper.java)·[BoardRenderer](../../ui/BoardRenderer.java) | 클릭 좌표를 모델에 넘기고 `Neighborhood`로부터 셀과 활성 블록 상태를 읽는다. `liveCells()`는 생존, `cancerCells()`는 암 여부 조회에 사용할 수 있다. |

### 시그니처

클래식·암세포 정사각형 보드에 제공하는 `com.holub.life.model`의 공개 타입이다.

```java
public interface Board {
  int rows();
  int columns();
  boolean step();
  void toggle(CellPosition position);
  void clear();
  BoardSnapshot snapshot();
  void restore(BoardSnapshot snapshot);
}
```

```java
public record CellPosition(int row, int column) {}
```

```java
import java.util.Set;

public record BoardSnapshot(
    int rows, int columns, Set<CellPosition> liveCells, Set<CellPosition> cancerCells) {
  public BoardSnapshot(int rows, int columns, Set<CellPosition> liveCells) {
    this(rows, columns, liveCells, Set.of());
  }
}
```

`CompositeBoard`는 `Board`를 구현하고 생성자 `CompositeBoard(Neighborhood neighborhood)`로 보드를 받는다.

### 계약

- **크기와 좌표.** `rows()`와 `columns()`는 양수이며 보드 생성 후 고정된다. `CellPosition`은 왼쪽 위 `(0, 0)`부터 행은 아래로, 열은 오른쪽으로 센다. 좌표 값은 보드 크기를 모르므로 사용 지점에서 `0 <= row < rows`, `0 <= column < columns`를 검사한다. `toggle`은 유효한 칸의 생사를 뒤집는다. `null`은 `NullPointerException`, 범위 밖은 `IndexOutOfBoundsException`으로 상태 변경 전에 거부한다. `clear()`는 모든 칸의 생사와 암 상태를 비운다. `toggle()`의 암 상태 처리와 세대 규칙은 기존 `Resident` 구현을 따른다.
- **세대 진행.** `step()`은 직전 세대를 기준으로 다음 세대를 계산하고 한 번에 반영한다. 클래식 규칙에서 보드 밖 이웃은 죽은 셀이다. 반환값은 생사 또는 암 여부가 바뀐 셀이 하나라도 있으면 `true`다. `false`도 한 세대를 진행한 결과이며 실행 정지나 렌더링 갱신 여부는 호출부에서 결정한다.
- **스냅샷 값.** `liveCells`는 일반 세포와 암세포를 모두 포함하고, `cancerCells`는 그 부분집합이다. 생존 집합에 없는 칸은 죽은 상태다. 두 집합은 각각 `Set.copyOf`로 방어 복사되며 순서는 의미가 없다. 양수 크기·생존 좌표 범위·암세포 부분집합 조건을 생성 시 검사한다. `null` 집합·원소는 `NullPointerException`, 잘못된 크기·좌표·부분집합은 `IllegalArgumentException`이다. 기존 3인자 생성자는 `cancerCells = Set.of()`로 위임한다. 레코드의 동등성에는 암세포 집합도 포함된다.
- **캡처.** `snapshot()`은 편집이나 세대 전환이 완료된 시점의 크기·생사·암 여부를 담는다. 하위 셀을 한 번 읽기 전용으로 순회하며, 셀 상태와 블록 활성 정보는 바꾸지 않는다. 활성 정보를 갱신할 수 있는 기존 `createMemento()`·`transfer(STORE)`는 호출하지 않는다.
- **복원.** `restore`는 같은 크기의 스냅샷으로 전체 생사·암 배치를 교체한다. `null`은 `NullPointerException`, 크기 불일치는 `IllegalArgumentException`으로 상태 변경 전에 거부한다. 검증된 스냅샷으로 보드를 비운 뒤 `Neighborhood.toggle()`로 모든 생존 칸을 배치해 상위 블록·경계를 활성화하고, 암세포 좌표에 `setCancer()`를 적용한다. 빈 암세포 집합은 기존 암 상태를 제거한다. 복원 직후 첫 세대도 경계 너머의 이웃을 계산한다.
- **실행 흐름.** `Board` 호출은 B가 직렬화한다. 자동 tick, 수동 tick, 편집, 캡처·복원은 서로 겹치지 않아야 한다. D에 스냅샷을 제공하는 경우 완료된 상태를 전달한다. 기존 `Clock.stop()`은 예약을 취소하며 진행 중인 tick은 끝까지 실행될 수 있다.

이번 확장에서도 기존 Composite를 감싸는 Facade를 유지한다. 최초 도입 이유와 비용은 [설계 결정](../../../../../../../../docs/GTY.md#002-클래식-보드-facade)에 기록한다.

### 후속 작업 인계

| 담당 | 작업할 때 적용할 기준 |
| --- | --- |
| A 모델 | `CompositeBoard`를 제공한다. `BOARD-01`~`BOARD-09`·`SNAP-C01`~`SNAP-C09`와 기존 모델 테스트를 검증 기준으로 삼는다. |
| B 실행 | `step()` 반환값은 생사·암 상태 변경 여부로 해석한다. 타이머 진행과 사용자 편집·캡처·복원은 한 번에 하나씩 실행하고, 기존 화면 갱신 정책은 실행 계층에서 유지한다. |
| C 저장 | 기존 `.life` 경로는 모델 도입 단계에서 유지한다. 연결 시 `Collection<Point>`와 `x = column`, `y = row` 매핑을 사용하고, 크기는 대상 보드에서 받는다. 기존 읽기 동작에 맞춰 중복은 한 칸으로 처리하고 범위 밖 좌표는 건너뛴다. 파일 출력은 기존 순회 순서를 따른다. 3인자 생성자는 기존 클래식 변환 코드와 소스 호환된다. 암세포를 지원하는 저장 경로는 `cancerCells()`도 전달해야 하며, 클래식 전용 코덱은 암세포를 무시해 저장하면 안 된다. |
| D 화면 | 클릭의 픽셀 좌표는 UI에서 행·열로 변환한다. 생사만 조회하는 곳에서는 `BoardSnapshot`을 사용하고, 활성 테두리와 부분 갱신은 기존 렌더링 경로에서 처리한다. |
| 2인용 | `BoardSnapshot`은 생사·암 상태를 표현한다. 셀 소유자는 포함하지 않으며 2인용은 [#5](https://github.com/DesignPattern8/DPTermProject/issues/5)에서 도입한 전용 보드 계약을 사용한다. |

각 도메인은 이 표를 참고해 자기 작업을 진행한다. 기존 파일 호환성, 실행·렌더링 동작, 공개 시그니처 또는 2인용 상태 표현을 바꿔야 할 때만 영향받는 담당자와 해당 이슈나 PR에서 조율한다. 결정이 바뀌면 이 문서에 내용과 근거 링크를 남긴다.

[클래식 보드 API 테스트](test-cases.md#클래식-보드-api-49)와 [암세포 스냅샷 테스트](test-cases.md#암세포-스냅샷-73)에서 검증 기준을 확인할 수 있다.

기존 3인자 생성 호출과 `Board` 메서드 시그니처는 유지한다. 레코드 구성 요소는 3개에서 4개로 바뀌므로 레코드 패턴·리플렉션으로 구성 요소 개수에 의존하는 사용자는 변경이 필요하다. 현재 저장소의 Java 호출부에는 해당 사용이 없다. 이 스냅샷은 보드 셀 배치 값이며, 게임의 세대 수·치료 횟수·승패 상태나 파일 형식을 정의하지 않는다.
