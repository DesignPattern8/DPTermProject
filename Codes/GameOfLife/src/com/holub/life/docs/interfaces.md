# 공통(core) 인터페이스 변경 제안 (초안)

> 이 문서는 회의 자료용 **초안**이다. 아래 변경은 아직 코드에 반영하지 않았다.
> 제안 주도: 2인용 모드. `Cell`은 1인용 모드·암세포 모드도 함께 쓰므로 회의에서 확정한다.
> 근거와 대안 비교: [001-cell-state-representation.md](001-cell-state-representation.md) (후보 C 기준)
> 코드 근거: #25(도메인 구조 변경) 이후. `Cell`·`Resident`·`Neighborhood`는 `com.holub.life.model`에 있고, 현재 모델 계약은 [../model/docs/interfaces.md](../model/docs/interfaces.md)에 있다. 아래 제안이 채택되면 그 문서에 합친다.

---

# CellState

- 제공 도메인: 공통(core, `com.holub.life.model`) / 사용 도메인: 2인용 모드, 1인용 모드, 암세포 모드(예정), UI 렌더러(`com.holub.life.ui`) / 담당자: 회의에서 결정
- 변경 이력:
  - 2026-11-03, 초안 작성 (Son2kwon), 합의 전
  - 2026-11-04, #25 이후 패키지(`com.holub.life.model`)로 갱신, 렌더러를 사용 도메인에 추가

## 시그니처

```java
package com.holub.life.model;

/** 셀 하나의 현재 상태. 모드마다 이 인터페이스를 구현한 enum을 하나씩 둔다. */
public interface CellState {
  /** 이웃 수를 셀 때 이 상태를 "살아 있음"으로 칠지. */
  boolean isAlive();
}

/** 클래식(B3/S23) 모드와 보드 바깥(Cell.DUMMY)이 쓰는 상태. */
public enum ClassicState implements CellState {
  DEAD(false), ALIVE(true);
  ...
}
```

2인용 모드는 자기 패키지에 둔다. 이 때문에 `twoplayer`가 처음으로 `life/model`에 의존하게 된다(001 회의 질문 7).

```java
package com.holub.life.twoplayer;

import com.holub.life.model.CellState;

public enum TwoPlayerState implements CellState {
  DEAD(false), A(true), B(true);
  ...
}
```

## 계약

- `isAlive()`는 상태마다 고정된 값을 반환한다(부작용 없음, 스레드 안전).
- 상태 비교는 동일성(`==`)으로 한다. 모드가 다른 상태끼리는 같지 않다(`ClassicState.DEAD != TwoPlayerState.DEAD`). 그래서 "죽었는지"는 `== DEAD`가 아니라 `isAlive()`로 판단해야 한다.

---

# Cell#state() (추가)

- 제공 도메인: 공통(core, `com.holub.life.model`) / 사용 도메인: 2인용 모드(이웃 색 조회), 승패 판정 집계(예정), UI 렌더러(셀 색 고르기)
- 변경 이력:
  - 2026-11-03, 초안 작성 (Son2kwon), 합의 전
  - 2026-11-04, #25 이후 경로로 갱신, 렌더러 사용처와 영향 범위 추가

## 시그니처

```java
public interface Cell {
  // 기존 메서드는 그대로 둔다 (isAlive() 포함)

  /** 이 셀의 현재 상태. figureNextState()로 계산만 하고 아직 transition()하지 않은 다음 상태가 아니다. */
  CellState state();
}
```

구현체별 동작(안):

| 구현체 | 반환값 |
| --- | --- |
| `Resident` | `amAlive ? ClassicState.ALIVE : ClassicState.DEAD` |
| `Cell.DUMMY` | 항상 `ClassicState.DEAD` |
| `Neighborhood` | 회의에서 결정 (`UnsupportedOperationException` 또는 집계 의미 부여) |
| 2인용 셀 (신규) | `TwoPlayerState.DEAD` / `A` / `B` |

## 계약

- 사전조건: 없음.
- 사후조건:
  - `null`을 반환하지 않는다.
  - `state().isAlive() == isAlive()` (단일 셀 구현체 기준. `Neighborhood`는 회의 결과에 따름).
  - 현재 상태를 반환한다. 한 세대 안에서 모든 셀이 `figureNextState()`를 마칠 때까지는 어떤 셀의 `state()`도 바뀌지 않는다. 기존 2단계 구조(`figureNextState` → `transition`)가 이를 보장한다.
- 예외: 단일 셀 구현체는 던지지 않는다. `Neighborhood`는 미정.
- 스레드: `isAlive()`와 같다. Clock tick 스레드에서 `figureNextState()` 안에서 호출된다. 화면 갱신(`ui/BoardRenderer`)은 `Neighborhood.beginRedraw()`가 확인하는 기존 `readingPermitted` 조건(`life/model/Neighborhood.java:28`, `293-297`) 아래에서 읽는다.

## 영향 범위

- 고칠 파일 (모델): `life/model/Cell.java`(메서드 선언 + `DUMMY`), `life/model/Resident.java`, `life/model/Neighborhood.java`
- 고칠 파일 (렌더러): `life/ui/BoardRenderer.java`. 지금은 `instanceof Resident`일 때만 `isAlive()`로 색을 고른다(`BoardRenderer.java:39-41`). "`Neighborhood`가 아닌 셀"로 넓히고 `state()`로 색을 고르게 바꾼다. 팔레트를 넘기는 방법과 담당은 회의에서 정한다(001 회의 질문 5). `ui`는 #9 담당 영역이다.
- 새 파일: `life/model/CellState.java`, `life/model/ClassicState.java` (공통), `life/twoplayer/TwoPlayerState.java` (2인용)
- 클래식 모드의 이웃 수 계산(`Resident.figureNextState`)은 바꾸지 않는다. 기존 특성 테스트가 그대로 통과해야 한다.
