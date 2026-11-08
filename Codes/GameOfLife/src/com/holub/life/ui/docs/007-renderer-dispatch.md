# [007] 렌더러의 셀 타입 분기

- 상태: 채택 (현행 유지)
- 작성자: D 담당 / 2026-11-08 / 이슈 #9, 브랜치 `refactor/D-ui`
- 기준 코드: `refactor/44-b-application` `b543703` (#45)
- 대체하는 문서: [002-board-rendering](002-board-rendering.md) (#25 이전 구조 기준 Decorator 초안)

## 문제

- `BoardRenderer.redraw`가 `instanceof Neighborhood` / `instanceof Resident`로 모델 구체 클래스를 분기한다(`BoardRenderer.java:14,39`). `GridCoordinateMapper.position`도 같다(`GridCoordinateMapper.java:13`).
- 새 셀 종류(2인용 셀 #5, 암세포 등)가 생기면 이 분기에 `else if`를 더해야 하고, 빠뜨리면 그 셀은 그려지지 않는다(컴파일 오류 없음).
- `ui`가 `model`의 구체 클래스와 그리기용 공개 메서드(`beginRedraw`, `childAt`, `gridSize`, `isActive`)를 안다.

## 후보

### A. Visitor
- `Cell`에 `accept(CellVisitor)`를 두고 렌더러·좌표 변환이 Visitor가 된다.
- 단점: `Cell`·`Neighborhood`·`Resident`는 A 도메인(`model`)이다. `Cell` 인터페이스 변경은 2인용(#5)에서도 회의 안건이라 D가 단독으로 바꿀 수 없다. 이중 디스패치 +1.

### B. Decorator 레이어 (초안 002)
- 셀 레이어 → 블록 테두리 → 활성 블록 테두리를 각각 보드 전체에 덧그린다.
- 단점: 레이어마다 블록을 다시 순회해야 하는데, 블록 순회 입구인 `Neighborhood.beginRedraw(drawAll)`이 **호출할 때마다 상태를 바꾼다**. 안정화 직후 마지막 갱신 표시(`oneLastRefreshRequired`)를 첫 호출에서 지우고(`Neighborhood.java:295`), 계산 중이면 `ConditionVariable`을 기다린다(`:296-297`). 레이어 3개가 각각 부르면 두 번째 레이어부터 그 블록을 건너뛰어 부분 갱신(`drawAll=false`) 결과가 달라진다. `RenderingExtractionTest#stableBlockGetsItsLastRefresh`(EXT-R05)가 이 동작을 고정하고 있다. 초안 002의 픽셀 일치 확인은 전체 그리기(`drawAll=true`)와 A의 `BoardSnapshot`(아직 없음)을 전제로 한 것이었다.

### C. Strategy: 셀 종류별 페인터 (`Map<Class, CellPainter>`)
- 단점: 분기가 `instanceof`에서 맵 조회로 옮겨질 뿐 새 셀 종류마다 등록이 필요하다는 점은 같다. 타입 +2 이상.

### D. 현행 유지
- 장점: 원본 `redraw` 순서·부분 갱신과 픽셀 SHA-256이 이미 테스트로 고정돼 있다(EXT-R01~05). 셀 종류는 지금 2개뿐이다.
- 단점: 위 문제가 남는다.

## 오버헤드 비교표

| 항목 | A. Visitor | B. Decorator | C. Strategy | D. 유지 |
| --- | --- | --- | --- | --- |
| 구조 비용 | 인터페이스 +1, `model` 3파일 수정 | 타입 +4 (초안 기준) | 타입 +2 이상 | 0 |
| 간접성 | 이중 디스패치 +1 | 레이어 체인 +2 | 맵 조회 +1 | 0 |
| 결합도 | `model` → Visitor 인터페이스 의존이 새로 생김 | `model` 구체 타입 의존 그대로 | 같음 | 같음 |
| 런타임 비용 | 같음 | 보드 순회 1회 → 3회 | 같음 | — |
| 동작 보존 | 가능 | **부분 갱신이 달라짐** (`beginRedraw` 상태 소비) | 가능 | 가능 |
| 테스트 용이성 | 같음 | 레이어별 테스트 가능 | 같음 | EXT-R01~05 |
| 확장 비용 (셀 종류 1개) | Visitor 메서드 1 + 모든 Visitor 구현 | 레이어 수정 | 페인터 1 + 등록 1 | `else if` 1 (렌더러·좌표 변환 2곳) |

## 결정과 근거

이번에는 D(현행 유지)로 둔다.
- B는 기존 동작(부분 갱신)을 바꾸므로 리팩토링 범위가 아니다.
- A는 A 도메인 인터페이스 변경이 필요하다. C는 문제를 옮기기만 한다.
- 새 셀 종류가 실제로 들어오는 시점(#5 2인용 셀)에 A의 `Cell` 상태 표현 결정(`docs/001-cell-state-representation.md`, `feature/two-player-mode` 브랜치)에 맞춰 다시 비교한다.

## 후속 작업

- A가 `BoardSnapshot`(또는 셀 상태 조회 API)을 만들면 렌더러가 스냅샷만 보고 그리도록 바꾸는 안을 다시 검토한다. 그때 `beginRedraw`의 부분 갱신 규칙을 스냅샷이 어떻게 전달할지 A와 합의해야 한다.
