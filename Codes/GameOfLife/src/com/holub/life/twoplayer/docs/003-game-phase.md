# [003] 게임 단계(초기 배치·시뮬레이션·개입·종료) 표현

- 상태: 채택
- 작성자: Son2kwon / 2026-11-03 / 관련 이슈: #6
- 근거 명세: [rules.md](rules.md) 4장(게임 진행), 5장(배치), 6장(종료)

## 문제

2인용 모드는 네 단계를 오가며, **단계마다 받는 입력과 다음 단계로 넘어가는 조건이 다르다**(rules.md 4장 표).

| 입력 | 초기 배치 | 시뮬레이션 | 개입 | 종료 |
| --- | --- | --- | --- | --- |
| 칸 선택(임시 배치) | 자기 진영만 | 거부 | 어디든 | 거부 |
| 확정 | N개 다 두면 시뮬레이션으로 | 거부 | 연속 패스 카운트 초기화 | 거부 |
| 패스 | 거부 | 거부 | 연달아 2번이면 다음 라운드로 | 거부 |
| tick (1세대) | 무시 | K세대면 개입 또는 종료로 | 무시 | 무시 |

기존 코드에는 단계 개념이 없다. `Universe`는 클릭을 항상 토글로 보내고(`Universe.java:79`), Clock 리스너는 tick마다 무조건 세대를 진행한다(`Universe.java:127-141`). 그래서 "지금 어떤 입력을 받을 수 있는가"를 판단할 곳을 새로 만들어야 한다.

## 후보

### A. State 패턴 — 단계마다 클래스 하나

```java
interface Phase {                         // package-private
  Response select(TwoPlayerGame g, Player who, int row, int col);
  Response confirm(TwoPlayerGame g, Player who);
  Response pass(TwoPlayerGame g, Player who);
  boolean tick(TwoPlayerGame g);
  PhaseKind kind();
}
// PlacementPhase, SimulationPhase, InterventionPhase, GameOverPhase
// TwoPlayerGame은 현재 Phase에 위임하고, Phase가 다음 Phase로 바꾼다.
```

- 장점: 한 단계의 규칙(무엇을 받고 언제 넘어가는가)이 한 클래스에 모인다. 표의 열 하나 = 클래스 하나. 단계를 추가할 때 기존 단계 클래스는 전이 지점만 고친다.
- 단점: 클래스가 4~6개 늘고, 입력 하나에 위임 호출이 1단계 생긴다. 초기 배치와 개입은 "임시 배치 → 확정" 로직을 공유하므로 공통 상위 클래스가 필요하다.

### B. enum + switch — 단계는 값, 규칙은 게임 클래스 안

```java
enum PhaseKind { PLACEMENT, SIMULATION, INTERVENTION, GAME_OVER }
Response select(...) { switch (phase) { case PLACEMENT: ...; case INTERVENTION: ...; default: return WRONG_PHASE; } }
// confirm, pass, tick도 각각 switch
```

- 장점: 클래스가 가장 적고, 흐름이 한 파일에 다 보인다. 위임 단계가 없다.
- 단점: 표의 행(입력) 기준으로 코드가 나뉘어서, 한 단계의 규칙이 4개 메서드의 switch에 흩어진다. 단계를 추가하면 모든 switch를 고쳐야 한다. 단계별 상태(남은 개수, 연속 패스 수, 세대 수)가 한 클래스의 필드로 모여 "지금 단계에서만 의미 있는 필드"가 섞인다.

### C. Strategy 패턴 — 단계 객체는 규칙만, 전이는 게임 클래스가 결정

A와 구조는 같지만, 다음 단계를 정하는 일을 `TwoPlayerGame`이 한다(단계 객체는 "끝났다"만 알림).

- 장점: 전이 순서(배치 → 시뮬레이션 → 개입 → … → 종료)가 한 곳에 모인다.
- 단점: 전이 조건(K세대, 연속 2패스, 양쪽 N개)은 각 단계 안에서 알게 되므로, 단계가 "끝났다"를 알리고 게임이 다시 판단하는 왕복이 생긴다. 이 게임은 전이 조건이 단계마다 다르고 순서가 단순해서 이 분리의 이득이 작다.

## 오버헤드 비교표

구현 전 **추정**이다. 채택안(A)은 구현 후 실측값으로 아래 "구현 후 실측"에 다시 적는다.

| 항목 | A. State | B. enum + switch | C. Strategy |
| --- | --- | --- | --- |
| 구조 비용 — 새 타입 | 인터페이스 1 + 단계 4 + 공통 상위 1 + 종류 enum 1 (추정) | enum 1 | A와 같음 |
| 구조 비용 — LOC | 약 300 (추정) | 약 220 (추정) | 약 320 (추정) |
| 간접성 | 입력 1번에 위임 +1 (`game` → `phase`) | 0 (switch 분기) | +1, 전이 시 왕복 +1 |
| 결합도 | 단계 클래스 ↔ `TwoPlayerGame` 상호 참조(package-private) | 게임 클래스 1개에 모두 | A와 같음 |
| `java.awt`/Swing 의존 | 없음 | 없음 | 없음 |
| 테스트 용이성 | Swing 없이 가능. 보드는 인터페이스 대역으로 교체 | 같음 | 같음 |
| 확장 비용 — 새 단계 1개 추가 (예: 개입 전 "확인" 단계) | 새 클래스 1 + 전이 지점 1곳 | 4개 메서드의 switch 전부 + 그 단계용 필드 | 새 클래스 1 + 게임 클래스의 전이 순서 |
| 확장 비용 — 새 입력 1개 추가 (예: 되돌리기) | `Phase`와 모든 단계 클래스 | 새 메서드 1개(switch 1개) | A와 같음 |

## 결정과 근거

**A(State)를 채택한다.**

- 이 게임에서 자주 바뀌는 쪽은 "단계별 규칙"이다. rules.md의 결정 사항(11/02, 11/03)도 대부분 특정 단계의 규칙(개입 선공, 빈 확정 거부, 자동 패스)이었다. A는 이런 변경이 한 클래스 안에서 끝난다.
- 입력 종류(선택·확정·패스·tick)는 rules.md에서 정해져 있고 늘어날 계획이 없다(되돌리기는 5장에서 금지). 그래서 A의 약점(새 입력 추가 비용)이 실제로 문제 될 일이 적다.
- B는 코드가 가장 짧지만, 단계별 필드(남은 개수, 연속 패스 수, 세대 수)가 한 클래스에 섞이고, 각 메서드가 네 단계를 모두 다룬다. 단계 전환 테스트(TP-F)가 실패했을 때 고칠 곳이 4개 메서드로 흩어진다.
- C는 A에 왕복 한 단계를 더하는데, 전이 순서가 고정이라 얻는 것이 없다.

## 셀 상태와의 분리

셀 상태 표현은 Cell 인터페이스 회의(#5, [001](../../docs/001-cell-state-representation.md)) 전이라 아직 정할 수 없다. 단계 로직이 보드에 묻는 것은 아래 다섯 가지뿐이므로, 이를 `TwoPlayerBoard` 인터페이스로 숨기고 테스트에서는 대역(Stub)을 쓴다. 실제 구현(셀 상태를 쓰는 보드)은 #5 이후에 붙인다. 계약은 [interfaces.md](interfaces.md).

| 단계 로직이 필요한 것 | `TwoPlayerBoard` 메서드 |
| --- | --- |
| 보드 크기 (진영 경계, 범위 검사) | `rows()`, `columns()` |
| 그 칸에 둘 수 있는가 (`DEAD`인가) | `isEmpty(row, col)` |
| 둘 빈칸이 하나라도 있는가 (자동 패스) | `hasEmptyCell()` |
| 확정한 셀 놓기 | `place(row, col, owner)` |
| 1세대 진행 | `advanceGeneration()` |

## 이번 범위와 남은 것

- 이번(#6 1차): 단계 전환, 차례, 선공 교대, 임시 배치·확정, 패스·자동 패스, 남은 개수, 라운드, tick, 설정값.
- 다음(#6 2차): 시뮬레이션 중 매 세대 전멸 검사와 승패 판정(rules.md 6장, TP-E). 셀 수 집계(`count(owner)`)가 필요해서 [005] 설계 결정과 함께 한다.

## 구현 후 실측

#6 1차 구현 기준(2026-11-03). LOC는 빈 줄과 주석을 뺀 줄 수.

| 항목 | 추정 (A) | 실측 |
| --- | --- | --- |
| 새 타입 | 인터페이스 1 + 단계 4 + 공통 상위 1 + 종류 enum 1 | 단계 쪽 6개: `Phase`(인터페이스), `PlacingPhase`(공통 상위), `InitialPlacementPhase`, `InterventionPhase`, `SimulationPhase`, `GameOverPhase` (모두 package-private) + 공개 타입 7개: `TwoPlayerGame`, `TwoPlayerBoard`, `GameSettings`, `Player`, `PhaseKind`, `Response`, `Coordinate` |
| LOC | 약 300 | 패키지 전체 367 (단계 클래스 6개 227, `TwoPlayerGame` 70, 나머지 70). 원래 줄 수(빈 줄·주석 포함) 529 |
| 간접성 | 입력 1번에 +1 | +1 (`TwoPlayerGame.select` → `Phase.select`) |
| 결합도 | 단계 ↔ 게임 상호 참조 | 단계 클래스는 `TwoPlayerGame`의 package-private 메서드 6개(`board`, `settings`, `countGeneration`, `startSimulation`, `endSimulation`, `endIntervention`)만 쓴다. 기존 `com.holub.life` 클래스에는 의존하지 않는다 |
| `java.awt`/Swing 의존 | 없음 | 없음 (`twoplayer` 패키지에 `java.awt`·`javax.swing` import 0. 처음에 임시 배치 좌표로 `java.awt.Point`를 썼다가 `Coordinate` record로 바꿈) |
| 테스트 | — | TP-F 17개, TP-P 15개, 모두 `StubBoard`로 Swing 없이 통과. 테스트 코드 605줄(대역·도우미 포함) |

- 기존 코드 변경: 없음(새 패키지만 추가). 클래식 특성 테스트 34개는 그대로 통과.
