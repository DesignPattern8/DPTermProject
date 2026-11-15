# [004] 플레이어 입력(칸 선택·확정·패스) 전달 방식

- 상태: 채택
- 작성자: Son2kwon / 2026-11-03 / 관련 이슈: #6
- 근거 명세: [rules.md](rules.md) 4장, 5장

## 문제

플레이어 입력은 네 가지다: 칸 선택(임시 배치·이동), 확정(버튼·Enter), 패스(버튼), tick(시뮬레이션 1세대). 이 입력을 UI(#7)에서 게임 모델로 어떻게 넘길지 정해야 한다.

- 입력은 거부될 수 있고, 거부 이유를 상태 표시줄에 보여줘야 한다(rules.md 5장 "불가능한 클릭: 무시하고 상태 표시줄에 이유 표시").
- 확정한 배치는 취소할 수 없다(rules.md 5장). 즉 되돌리기(undo)는 요구사항이 아니라 **금지 사항**이다.
- hot-seat 한 화면이라 입력은 항상 같은 프로세스·같은 스레드(Swing 이벤트 스레드)에서 들어온다. 네트워크·큐가 없다.
- rules.md 4장은 Command(`PlaceCellCommand`, `PassCommand`)를 후보로 적어 두었다(Clock 리팩토링의 Command 구조와 맞추자는 이유).

## 후보

### A. Command 패턴 — 입력마다 명령 객체

```java
interface GameCommand { Response execute(TwoPlayerGame g); }
final class SelectCellCommand implements GameCommand { ... }   // 칸 선택
final class ConfirmCommand implements GameCommand { ... }
final class PassCommand implements GameCommand { ... }
// UI: game.execute(new PassCommand(Player.A));
```

- 장점: 입력을 객체로 저장·기록·재생할 수 있다. 명령 기록(로그)을 남기거나, 테스트에서 입력 시퀀스를 목록으로 만들 수 있다. undo를 붙이기 쉽다.
- 단점: 이 게임은 undo가 금지이고, 큐·재생·원격 전달도 없다. 명령 클래스 3개와 인터페이스 1개가 늘지만 하는 일은 `game`의 메서드 하나를 부르는 것뿐이다. 거부 이유를 돌려받는 것은 B와 같다.

### B. 모델 메서드 직접 호출 + 결과 값

```java
Response select(Player who, int row, int col);
Response confirm(Player who);
Response pass(Player who);
boolean tick();
// Response: ACCEPTED 또는 거부 이유(WRONG_PHASE, NOT_YOUR_TURN, CELL_NOT_EMPTY, ...)
```

- 장점: 클래스가 늘지 않고, 호출 경로가 가장 짧다. 테스트는 메서드 호출 순서가 곧 입력 시퀀스다.
- 단점: 입력을 객체로 저장할 수 없다. 나중에 기록·재생이 필요하면 바꿔야 한다.

### C. Observer로 입력 이벤트를 발행

UI가 입력 이벤트를 발행하고 게임 모델이 구독한다(기존 `Publisher` 재사용).

- 장점: UI가 모델을 몰라도 된다.
- 단점: 입력마다 거부 이유를 **돌려받아야** 하는데, 발행-구독은 반환값이 없어서 결과를 다시 다른 이벤트로 돌려줘야 한다. 이 경우는 UI가 모델을 직접 부르는 쪽이 자연스럽다. (모델 → UI 방향의 상태 표시줄 갱신은 Observer 후보로 #7에서 따로 다룬다.)

## 오버헤드 비교표

구현 전 **추정**이다.

| 항목 | A. Command | B. 직접 호출 | C. Observer |
| --- | --- | --- | --- |
| 구조 비용 — 새 타입 | 인터페이스 1 + 명령 3 | 결과 enum 1 | 이벤트 타입 1~3 + 결과 이벤트 |
| 구조 비용 — LOC | B + 약 60 (추정) | 기준 | B + 약 80 (추정) |
| 간접성 | 입력 1번에 +1 (`execute` → 게임 메서드) | 0 | +1 (발행 → 구독자), 결과는 별도 경로 |
| 결합도 | UI → 명령 클래스 → 게임 | UI → 게임 | UI → Publisher ← 게임 |
| 테스트 용이성 | Swing 없이 가능 | Swing 없이 가능 | 가능하나 결과 확인에 구독자 대역 필요 |
| 확장 비용 — undo·기록 추가 | 명령에 `undo()`·기록만 추가 | 메서드를 명령으로 감싸는 작업 필요 | 이벤트 기록은 쉬움, undo는 어려움 |
| 확장 비용 — 새 입력 1개 | 명령 클래스 1 + 게임 메서드 1 | 게임 메서드 1 | 이벤트 1 + 구독 처리 |

## 결정과 근거

**B(직접 호출 + `Response` 결과 값)를 채택한다.**

- Command가 값을 하는 경우(undo, 큐, 기록·재생, 원격 전달)가 이 게임에는 하나도 없다. undo는 규칙으로 금지돼 있고(rules.md 5장), hot-seat라 큐나 원격 전달도 없다. A를 고르면 메서드 호출 하나를 감싸는 클래스 3개가 생길 뿐이다.
- 거부 이유를 바로 돌려받아야 하므로 C는 맞지 않는다.
- rules.md 4장의 "Clock 리팩토링의 Command 구조와 맞추기"는 **UI 쪽**에서 할 수 있다. 메뉴·키 입력을 Command로 만드는 것은 UI(#7)나 Clock 도메인의 결정이고, 그 Command가 이 모델의 메서드를 부르면 된다. 모델이 Command를 알 필요는 없다.
- 기록·재생이 필요해지면(예: 발표 시연용 리플레이) 그때 이 메서드들을 감싸는 Command를 추가한다. 모델 메서드는 그대로 둘 수 있어서 바꾸는 비용이 작다.

## 후속 작업

- `Response`의 거부 이유 → 상태 표시줄 문구 변환은 UI(#7)에서 한다.
- #7에서 메뉴·키 입력을 Command로 만들기로 하면, 이 문서의 결정과 충돌하지 않는다(그 Command가 이 메서드를 호출).
