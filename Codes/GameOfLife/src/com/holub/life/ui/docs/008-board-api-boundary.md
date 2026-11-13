# [008] A의 Board API(`CellPosition`, `BoardSnapshot`)를 화면 어디까지 쓸 것인가

- 상태: 채택
- 작성자: D 담당 / 2026-11-13 / 이슈 #9, PR #46
- 기준 코드: `main` `9e16603` (#50 `Board`·`BoardSnapshot`·`CellPosition`, #56 `GameController`의 `Board` 연결)
- 선행: [007](007-renderer-dispatch.md) 후속 작업("A가 `BoardSnapshot`을 만들면 다시 검토"), A 인계표 [`model/docs/interfaces.md#후속-작업-인계`](../../model/docs/interfaces.md#후속-작업-인계)의 D 행

## 문제

A 인계표의 D 행: "클릭의 픽셀 좌표는 UI에서 행·열로 변환한다. 생사만 조회하는 곳에서는 `BoardSnapshot`을 사용하고, 활성 테두리와 부분 갱신은 기존 렌더링 경로에서 처리한다."

지금 D 코드:

| 위치 | 하는 일 | 쓰는 타입 |
| --- | --- | --- |
| `BoardPanel.java:51-52` | 클릭 픽셀 → `GridCoordinateMapper.position(...)` → `controller.toggle(position.y, position.x)` | `java.awt.Point` (x = 열, y = 행) |
| `GridCoordinateMapper.java:12-23` | 블록마다 정수 나눗셈으로 픽셀 → 셀 | `Point` |
| `BoardRenderer.java:13-48` | 셀 색(`cell.isAlive()`), 블록 테두리, 활성 테두리, 부분 갱신(`beginRedraw`) | `Cell` 트리 |

- 클릭 경로는 행·열을 `Point`에 담아 넘기고, 호출하는 쪽이 `y`를 행, `x`를 열로 **뒤집어** 꺼낸다(`BoardPanel.java:52`). A는 같은 뜻의 `CellPosition(row, column)`을 만들었고 B의 `GameController.toggle`도 안에서 `new CellPosition(row, column)`을 만든다.
- 렌더러의 생사 조회(`BoardRenderer.java:41`)는 인계표의 "생사만 조회하는 곳"에 해당하는지 판단이 필요하다. 같은 순회에서 활성 테두리·부분 갱신도 처리한다.
- 클릭 경로는 지금 JUnit이 없다. `NativeUiSmoke`(디스플레이 필요)만 본다.

## 후보

### A. 렌더러가 생사 색을 `BoardSnapshot`으로 그린다 (테두리·부분 갱신은 트리)
- 화면이 그릴 때마다 스냅샷을 받아 셀 색을 정하고, 블록 테두리·활성 테두리·`beginRedraw`는 지금처럼 트리를 돈다.
- 단점
  - A 계약상 `Board` 호출은 B가 직렬화한다(`model/docs/interfaces.md`, B `004` D8). UI가 `Board.snapshot()`을 직접 부르면 그 잠금을 건너뛴다. B에 `GameController.snapshot()`을 새로 요청해야 한다(B 공개 API 변경).
  - 부분 갱신(`refreshNow`, `BoardPanel.java:76-93`)은 tick이 끝난 뒤 이벤트 스레드에서 늦게 실행된다. 스냅샷을 찍는 시점과 트리의 `amActive`를 읽는 시점이 달라져, 한 프레임 안에서 색과 활성 테두리가 서로 다른 세대를 보여줄 수 있다. 지금은 한 순회에서 둘 다 읽는다.
  - 셀 트리 순회가 두 번(스냅샷 + 그리기) 된다.

### B. 클릭 경계만 `CellPosition`으로, 렌더러는 트리 유지
- `GridCoordinateMapper.cellAt(cell, pixel, surface) : CellPosition`을 추가하고 `BoardPanel`은 `controller.toggle(cell.row(), cell.column())`로 부른다. 계산은 기존 `position`을 그대로 쓴다(블록별 정수 나눗셈, 범위 밖 예외 유지).
- 기존 `position`/`userClicked`는 지우지 않는다. 특성 테스트(`RenderingExtractionTest` EXT-R04, `ClassicBoard`, `ClassicCellTest`)가 쓴다.
- 렌더러는 007 결정대로 둔다. 인계표도 "활성 테두리와 부분 갱신은 기존 렌더링 경로"라고 했고, 렌더러의 생사 조회는 그 순회 안에 있다.

### C. 현행 유지
- 행·열 뒤집기가 남고, 클릭 경로는 계속 JUnit 밖에 있다.

### 기타 검토
| 패턴 | 판단 | 이유 |
| --- | --- | --- |
| Adapter (`Point` ↔ `CellPosition`) | B에 포함 | `cellAt`이 기존 `position` 결과를 A의 값 타입으로 바꾼다. 별도 클래스는 만들지 않는다 |
| Observer 인자에 스냅샷 (`boardChanged(BoardSnapshot)`) | 보류 | B의 `GameListener` 시그니처 변경. A안과 같은 시점 문제가 있다. B가 2차 리팩토링에서 검토하기로 함(B `003` 후속 작업) |
| Proxy (UI용 읽기 전용 Board) | 기각 | 잠금은 B가 가진다. UI 쪽 Proxy가 잠금을 공유하려면 B 내부를 알아야 한다 |

## 오버헤드 비교표

| 항목 | A. 스냅샷 렌더링 | B. 클릭 경계만 | C. 유지 |
| --- | --- | --- | --- |
| 구조 비용 | B API +1(`snapshot()`), `BoardRenderer` 시그니처 변경, `BoardPanel` 수정 (추정) | 메서드 +1(`cellAt`), `BoardPanel` 1줄 | 0 |
| 간접성 | 그리기마다 B 잠금 → `snapshot()` 1단계 추가 | 클릭마다 `cellAt` → `position` 1단계 추가 | — |
| 결합도 | `ui` → `model.BoardSnapshot` + 트리 둘 다 | `ui` → `model.CellPosition` 추가 | 그대로 |
| 런타임 비용 (실측, 아래) | 그리기 1회당 스냅샷 0.017~0.082 ms 추가 (전체 그리기 1.72~1.76 ms의 1~5%) | 클릭 1회당 객체 1개 | — |
| 동작 보존 | 색·테두리가 다른 시점을 볼 수 있음 | 같음 (`position` 재사용) | 같음 |
| 테스트 용이성 | 스냅샷만으로 색 검사 가능 | 클릭 → 셀을 headless `BoardPanel`로 검사 가능(새 UI-R10~R12) | `NativeUiSmoke`만 |
| 확장 비용 | 2인용 소유자 색은 `liveCells`에 없어 별도 경로 필요(A 인계표 2인용 행) | 2인용 배치 클릭도 `CellPosition`으로 받음 | — |

### 런타임 측정

- 측정 코드: 저장소에 넣지 않은 임시 `SnapBench.java`. 64×64 `CompositeBoard`(8×8 블록)에서 `snapshot()`과 `BoardRenderer.redraw(..., drawAll=true)`(512×512 `BufferedImage`)를 각각 1,000번 실행했다. 2,000번 워밍업 후 10회 반복 중 최소값을 1회당 시간으로 나눴다.
- 환경: Linux, OpenJDK 21.0.12, 4코어, headless.

| 보드 | 살아 있는 셀 | `snapshot()` | 전체 그리기 |
| --- | --- | --- | --- |
| glider | 5 | 0.0174 ms | 1.7215 ms |
| 무작위(시드 42, 25%) | 993 | 0.0823 ms | 1.7602 ms |

스냅샷 비용은 A안을 막는 이유가 아니다. A안을 기각하는 이유는 B API 변경과 한 프레임 안의 시점 불일치다.

## 결정과 근거

B를 채택한다.
- 인계표의 "클릭 좌표는 UI에서 행·열로 변환" 항목을 A의 타입으로 마무리하고, 행·열 뒤집기(`position.y, position.x`)를 없앤다.
- 렌더러의 생사 조회는 활성 테두리·부분 갱신과 같은 순회에 묶여 있어 인계표의 "기존 렌더링 경로"로 본다. 007의 결정(현행 유지)을 이 근거로 유지한다.
- 렌더러를 스냅샷으로 옮기는 일은 B가 `GameListener`에 스냅샷을 싣거나 `snapshot()`을 제공할 때 다시 본다.

## 구현 결과

| 항목 | 전 | 후 |
| --- | --- | --- |
| `GridCoordinateMapper` 공개 메서드 | `position`, `userClicked` | + `cellAt` (`position` 결과를 `CellPosition`으로) |
| `BoardPanel` 클릭 | `controller.toggle(position.y, position.x)` | `controller.toggle(cell.row(), cell.column())` |
| `ui` 소스 전체 행 수 | 401 | 412 (+11) |
| 클릭 경로 headless JUnit | 0 | 4 (UI-R10~R13) |

- 테스트: `./gradlew test` 116개 통과(main 병합 후 112 + 새 4). 기존 테스트는 수정하지 않았다. Linux, OpenJDK 21(toolchain만 21로 바꾸는 init 스크립트).
- `NativeUiSmoke`(EXT-N01): 수정 없이 Xvfb에서 통과, 종료 코드 0.

## 후속 작업

- B와 협의: `GameListener.boardChanged`에 `BoardSnapshot`을 실을지(B `003` 후속 작업). 실리면 A안의 시점 문제가 사라지므로 렌더러를 다시 비교한다.
- 2인용(#5, #7): 소유자 색은 `BoardSnapshot.liveCells`에 없으므로 렌더러 확장은 #5의 상태 표현 결정 뒤에 한다.
