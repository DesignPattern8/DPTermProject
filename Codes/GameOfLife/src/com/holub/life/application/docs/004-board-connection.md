# [004] GameController를 A의 Board에 연결하고 보드 호출을 직렬화

- 상태: 채택
- 작성: 시형 / 2026-11-13 / [#55](https://github.com/DesignPattern8/DPTermProject/issues/55)
- 기준 코드: `refactor/44-b-application` `e19c3e8` (#45 + main의 #49·#52). 줄 번호는 이 커밋 기준이다.
- 선행: [003](003-pattern-analysis.md) P3·P8, A [클래식 보드 계약](../../model/docs/interfaces.md#클래식-보드-계약)

## 문제

1. **세대 진행 규약을 B가 안다 (003 P8).** `GameController.tick()`이 바깥 이웃 8칸을 `Cell.DUMMY`로 넘기고 `figureNextState` → `transition` 순서를 직접 호출한다(`GameController.java:45-60`). A가 #49에서 같은 일을 하는 `Board.step()`을 제공했다(`CompositeBoard.java:27-32`).
2. **보드 호출이 겹칠 수 있다 (003 P3).** 자동 tick은 `java.util.Timer` 스레드(`UtilTimerTickSource`), 클릭 편집·Clear·Load는 Swing 이벤트 스레드에서 같은 보드를 바꾼다. 셀 트리는 이를 막지 않는다. `Neighborhood`의 `readingPermitted`는 `static`이고(`Neighborhood.java:28`) 전이 중 그리기만 막는다(`Neighborhood.java:229,241,296-297`). A 계약은 "`Board` 호출은 B가 직렬화한다. 자동 tick, 수동 tick, 편집, 캡처·복원은 서로 겹치지 않아야 한다"고 정했다.
3. **파일 경로는 아직 `Cell`이 필요하다.** C의 `LegacyLifeCodec`은 `NeighborhoodState`(가변 Memento)를 주고받는다(`GameController.java:63-81`). C가 `BoardSnapshot` 경로를 연결하기 전까지 `createMemento`/`transfer`가 남는다.

## 후보

### D7. Board 연결

| 후보 | 구조 | 장점 | 단점 |
| --- | --- | --- | --- |
| A. `Cell` 직접 사용 유지 | 지금 그대로 | 변경 없음 | A가 Facade를 만든 이유(호출부가 계산 순서를 모르게)가 B에서 쓰이지 않는다. 테스트 대역을 넣을 수 없다 |
| B. 편집·진행은 `Board`, 파일은 기존 `Cell` | 생성자가 `Board`와 파일용 `Cell`을 받는다 | P8 해소. `Board` 대역으로 B만 테스트 가능. 파일 동작은 그대로 | 같은 보드를 두 타입으로 들고 있는 과도기 구조. C 연결 후 `Cell` 제거 필요 |
| C. C의 스냅샷 경로까지 기다렸다가 한 번에 | 전부 `Board`로 | 최종 구조 | C 작업에 묶인다. 직렬화(D8)도 미뤄진다 |

### D8. 보드 호출 직렬화

| 후보 | 구조 | 장점 | 단점 |
| --- | --- | --- | --- |
| A. 그대로 | 잠금 없음 | 변경 없음 | A 계약 위반, 겹침 가능 |
| B. 모니터 (`synchronized`, 패턴 없이) | `GameController`가 잠금 하나로 보드 호출을 감싼다. 알림은 잠금 밖에서 보낸다 | 호출이 동기로 남아 `load`의 `IOException` 전달 등 기존 흐름이 같다. 스레드는 그대로 | 진행 중인 tick이 끝날 때까지 이벤트 스레드의 편집이 기다린다(세대 계산 1번 길이) |
| C. Command 큐 (단일 작업 스레드) | 요청을 명령 객체로 만들어 한 스레드가 순서대로 실행 | 잠금이 없다. 실행 기록·재생으로 확장 가능 | 요청이 비동기가 되어 `load` 실패를 메뉴에 바로 알릴 수 없다. 스레드가 하나 더 생긴다. 명령 객체가 요청 종류만큼 늘어난다 |
| D. tick을 Swing 이벤트 스레드로 (`SwingTickSource`, D 소유) | 모든 보드 호출이 한 스레드 | 잠금이 필요 없다 | tick 스레드가 바뀌는 동작 변경. D의 브랜치 정리가 필요하다. 003 7장 5단계로 미뤄 둔 일 |

## 오버헤드 비교표

구현 후 실측(기준 `e19c3e8`, 물리 행 수). 추정은 표시했다.

| 항목 | D7-A | **D7-B** | D7-C |
| --- | --- | --- | --- |
| 구조 비용 | 0 | 타입 0, src 파일 2(`GameController.java` 86 → 96, `Life.java` 22 → 22), git +36 / -26. 테스트 파일 3(새 1) | 타입 0, C 작업 포함 (추정 파일 5 이상) |
| 간접성 | 0 | 편집·진행 +1 (`Board` → `Neighborhood`, A의 Facade 1단계) | 같음 |
| 결합도 | B → `Cell`, `Cell.DUMMY` | B → `Board`, `CellPosition` + 파일용 `Cell` | B → `Board`만 |
| 테스트 용이성 | 실제 셀 트리 필요 | `Board` 대역으로 B만 테스트 | 같음 |
| 확장 비용 | 다른 보드 구현 사용 불가 | `Board` 구현만 바꾸면 됨 (파일 경로 제외) | 같음 |

| 항목 | D8-A | **D8-B** | D8-C | D8-D |
| --- | --- | --- | --- | --- |
| 구조 비용 | 0 | 타입 0, 잠금 필드 1, `synchronized` 블록 5개 | 타입 +2 이상(명령, 실행기) 추정 | 타입 0, D 파일 교체 |
| 간접성 | 0 | 0 (잠금 획득만) | 요청당 +2 (명령 생성, 큐) 추정 | 0 |
| 런타임 | — | 잠금 경합은 tick과 편집이 겹칠 때만. 측정하지 않음 | 스레드 1개 추가 | — |
| 동작 변화 | 겹침 가능 | 없음 (겹침만 막힘) | 요청이 비동기가 됨 | tick 스레드 변경 |
| 테스트 용이성 | — | 대역 `Board`의 `step()`을 멈춰 두고 편집이 기다리는지 확인 | 큐 대역 필요 | Swing 필요 |

## 결정과 근거

- **D7-B.** `Board.step()`이 B가 들고 있던 세대 진행 규약을 가져가고(P8 해소), `Board` 대역으로 B 테스트가 셀 트리 없이 가능해진다. 파일 경로는 C의 결정 영역이라 손대지 않고, 과도기 `Cell` 인자는 C가 스냅샷 경로를 연결하면 지운다. D7-C는 C 일정에 묶여 직렬화까지 미뤄진다.
- **D8-B.** 호출 방식(동기)과 스레드를 그대로 둔 채 A 계약만 지킨다. D8-C(Command 큐)는 `load` 실패를 메뉴가 바로 받는 지금 흐름을 깨고, 큐·명령 객체가 필요한 기록·재생 요구가 없다. D8-D는 스레드를 바꾸는 일이라 D와 합의 후 별도로 한다.
- 알림(`GameListener`)은 잠금 밖에서 보낸다. 리스너가 다시 `GameController`를 부르거나 오래 걸려도 보드 잠금을 잡고 있지 않게 하기 위해서다.

## 전/후 비교

```java
// 전 (GameController.java:45-60)
if (outermostCell.figureNextState(Cell.DUMMY, ... 8개 ...)) changed = outermostCell.transition();

// 후
boolean changed;
synchronized (lock) { changed = board.step(); }
if (changed) notifyListeners(GameListener.Change.GENERATION);
```

클래스 관계는 [003 후 다이어그램](diagrams/003-after.svg)에서 `GameController --> Cell`이 `GameController --> Board`(+ 파일용 `Cell`)로 바뀐다. [004 다이어그램](diagrams/004-after.svg) 참고.

### 동작 보존

- `CompositeBoard.step()`은 전의 `tick()` 본문과 같은 호출(`figureNextState(DUMMY×8) && transition()`)이다.
- 범위 밖 편집: 전에는 `Neighborhood.toggle`이 `ArrayIndexOutOfBoundsException`, 후에는 `CompositeBoard.toggle`이 상태를 바꾸기 전에 `IndexOutOfBoundsException`(상위 타입)을 던진다. 둘 다 보드를 바꾸지 않고 이벤트 스레드로 올라간다. 지금 화면은 512px에서 범위 밖 좌표를 만들지 않는다(EXT-N01).
- 파일 저장·불러오기는 같은 `Cell` Memento 경로를 그대로 쓴다.

## 검증

- 테스트 97 → 103 (APP-B01~B06). 기존 테스트는 `GameController` 생성자 호출 3곳(`GameControllerTest`, `GameMenuTest`)만 바꿨고 기대값은 그대로다.
- APP-B05는 잠금을 지운 코드로 돌리면 실패하고(편집이 진행 중인 `step()` 안으로 들어감), 잠금이 있으면 통과하는 것을 확인했다.
- `NativeUiSmoke`(EXT-N01): 수정 없이 통과 (Windows 11 + Temurin 26.0.2).
- 다이어그램: [004-after.puml](diagrams/004-after.puml) ([svg](diagrams/004-after.svg)). 렌더링 때 다시 그려진 003·#25 결과 파일은 되돌렸다.

## 후속 작업

- C가 `BoardSnapshot` ↔ `.life` 경로를 제공하면 `load/store`를 `Board.snapshot/restore`로 옮기고 생성자의 `Cell` 인자를 지운다.
- `SwingTickSource` 도입(D8-D) 시 잠금이 필요 없어지는지 다시 판단한다. 잠금은 남겨도 동작에 영향이 없다.
- D 요청안의 `toggleCell(CellPosition)`은 D가 좌표 변환을 `CellPosition`으로 바꿀 때 함께 추가한다.
