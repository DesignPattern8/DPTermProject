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

## 공통 보드 계약 초안

- 상태: 제안. 2026-11-10, [#47](https://github.com/DesignPattern8/DPTermProject/issues/47), 기준 `main` (`34269bc`).
- 제공: A 모델(태영). 사용: B 실행·편집, C 저장·복원, D 화면. 셀 상태 표현은 2인용 담당과 함께 검토한다.
- 합의 이력: 초안 작성. 채택 시 결정, 담당자, 합의 링크를 기록한다.

| 사용처 | 현재 호출과 제안의 연결 지점 |
| --- | --- |
| B [GameController](../../application/GameController.java) | `Cell.toggle`·`clear` 호출과 `figureNextState` → `transition` 순서를 `Board`의 편집·`step()`에 연결한다. |
| C [LegacyLifeCodec](../../persistence/LegacyLifeCodec.java) | 가변 `NeighborhoodState`와 파일을 주고받는 경계에서 좌표 변환과 스냅샷 검증을 맡는다. |
| D [GridCoordinateMapper](../../ui/GridCoordinateMapper.java)·[BoardRenderer](../../ui/BoardRenderer.java) | 클릭을 `CellPosition`으로 해석하고, 스냅샷에서 생존 셀을 읽는다. 활성 블록 표시는 별도 합의가 필요하다. |

### 시그니처

`com.holub.life.model`에 둘 공개 타입의 시그니처 제안이다. 첫 적용 대상은 기존 클래식 정사각형 보드다.

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

public record BoardSnapshot(int rows, int columns, Set<CellPosition> liveCells) {}
```

### 계약

- **크기와 좌표.** `rows()`와 `columns()`는 양수이며 보드 생성 후 고정된다. `CellPosition`은 왼쪽 위 `(0, 0)`부터 행은 아래로, 열은 오른쪽으로 센다. 좌표 값은 보드 크기를 모르므로 사용 지점에서 `0 <= row < rows`, `0 <= column < columns`를 검사한다. `toggle`은 유효한 칸의 생사를 뒤집는다. `null`은 `NullPointerException`, 범위 밖은 `IndexOutOfBoundsException`으로 상태 변경 전에 거부한다. `clear()`는 모든 칸을 죽은 상태로 만든다.
- **세대 진행.** `step()`은 직전 세대를 기준으로 다음 세대를 계산하고 한 번에 반영한다. 클래식 규칙에서 보드 밖 이웃은 죽은 셀이다. 반환값은 생사가 바뀐 셀이 하나라도 있으면 `true`다. `false`도 한 세대를 진행한 결과이며 실행 정지나 렌더링 갱신 여부는 호출부에서 결정한다.
- **캡처.** `snapshot()`은 편집이나 세대 전환이 끝난 시점의 크기와 생존 좌표를 담는다. `BoardSnapshot`의 생성자는 양수 크기와 좌표 범위를 확인하고 `Set.copyOf`로 방어 복사한다. 집합에 없는 칸은 죽은 상태이며 순서에는 의미를 부여하지 않는다. `null` 인자·원소는 `NullPointerException`, 잘못된 크기·좌표는 `IllegalArgumentException`으로 거부하는 안이다. 기존 `createMemento()`의 `transfer(STORE)`는 블록 활성 정보를 갱신할 수 있다. 새 캡처에는 모델 상태를 유지하는 읽기 경로가 필요하다.
- **복원.** `restore`는 현재 보드와 크기가 같은 스냅샷으로 전체 배치를 교체한다. `null`은 `NullPointerException`, 크기 불일치는 `IllegalArgumentException`으로 상태 변경 전에 거부한다. 복원 직후 같은 배치를 직접 편집해 만든 보드와 다음 세대 결과가 같도록 블록 활성·경계 정보를 준비한다.
- **실행 흐름.** `Board` 자체의 동시 호출은 지원하지 않는 안이다. B는 자동 tick, 수동 tick, 편집, 캡처·복원을 한 실행 흐름에 직렬화하고, D에는 완료된 상태의 스냅샷을 전달한다. 기존 `Clock.stop()`은 예약을 취소하며 진행 중인 tick의 완료를 기다리는 용도로는 사용할 수 없다.

### 보드 진입점 후보

| 후보 | 추가 타입 수와 호출 단계(추정) | 영향 |
| --- | --- | --- |
| A. `Cell`을 계속 사용 | 좌표·스냅샷 2개. 보드 조작은 기존 호출 경로. | B가 계산·반영 순서와 `Cell.DUMMY`를 계속 다룬다. 보드 전체 연산과 하위 셀 연산이 같은 계약에 있다. |
| B. 기존 Composite를 감싸는 `Board` Facade | 좌표·스냅샷·인터페이스·구현 4개. 보드 조작에 위임 1단계. | B가 보드 단위 API만 호출한다. 기존 세대 계산과 블록 최적화를 재사용하고, Swing 없이 보드 계약을 검증할 수 있다. |

이 초안은 후보 B의 시그니처를 제시한다. 변경 파일 수·LOC·실행 비용은 구현 후 확인한다.

### 팀 합의 항목

| 담당 | 결정할 내용 |
| --- | --- |
| A·B | `step()` 반환값과 변경 알림 시점, 범위 오류를 처리할 계층, Timer·Swing 입력 사이에서 모델 호출을 직렬화할 방법을 확정한다. |
| A·B·C | 기존 `.life`의 `Collection<Point>`는 `Point.x = column`, `Point.y = row`로 대응한다. 파일에 보드 크기가 없으므로 읽을 때 대상 보드 크기를 제공하는 안을 검토한다. 예전 파일의 중복 좌표·범위 밖 좌표 처리와 쓰기 순서 호환 범위를 정한다. B는 읽기와 검증을 마친 스냅샷을 받아 복원하는 안을 검토한다. |
| A·D | 생존 좌표만으로는 파란 활성 테두리와 부분 갱신 상태를 재현할 수 없다. 첫 적용에서 기존 렌더링 경로를 사용하는 안과, 읽기 전용 표시 데이터가 필요한 시점·필드를 합의한다. |
| A·2인용 담당 | `liveCells`는 클래식 생사만 표현한다. [#5](https://github.com/DesignPattern8/DPTermProject/issues/5)의 셀 상태 표현과 기존 `TwoPlayerBoard`를 기준으로 소유자나 세포 종류가 필요한 모드의 스냅샷 공유 범위를 결정한다. |

후속 구현의 검증 기준은 [공통 보드 계약 테스트 초안](test-cases.md#공통-보드-계약-초안-47)에 정리한다.
