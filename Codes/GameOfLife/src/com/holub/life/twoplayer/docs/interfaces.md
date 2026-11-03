# 2인용 모드 인터페이스

- 근거: [003-game-phase.md](003-game-phase.md), [004-player-input.md](004-player-input.md)
- 공통 `Cell` 인터페이스 변경 제안은 core 쪽 문서에 있다: [../../docs/interfaces.md](../../docs/interfaces.md)

---

# TwoPlayerBoard

- 제공 도메인: 2인용 모드(구현은 #5 이후) / 사용 도메인: 2인용 게임 진행(`TwoPlayerGame`) / 담당자: Son2kwon
- 변경 이력:
  - 2026-11-03, 최초 작성 (#6). 셀 상태 회의(#5) 전이라 게임 진행이 보드에 묻는 것만 모았다.
  - 2026-11-03, `count(Player)` 추가 (#6 2차, 승패 판정)

## 시그니처

```java
package com.holub.life.twoplayer;

public interface TwoPlayerBoard {
  int rows();
  int columns();
  boolean isEmpty(int row, int column);
  boolean hasEmptyCell();
  void place(int row, int column, Player owner);
  void advanceGeneration();
  int count(Player owner);
}
```

## 계약

- `rows()`, `columns()`: 1 이상, 게임 중 바뀌지 않는다.
- `isEmpty(row, column)`: 그 칸이 `DEAD`이면 true. 사전조건: `0 <= row < rows()`, `0 <= column < columns()` (범위 검사는 호출하는 쪽이 먼저 한다).
- `hasEmptyCell()`: `isEmpty`가 true인 칸이 하나라도 있으면 true.
- `place(row, column, owner)`: 그 칸을 `owner`의 산 셀로 만든다. 사전조건: `isEmpty(row, column)`이 true. 게임 진행은 확정할 때만 부른다(임시 배치는 보드에 놓지 않는다).
- `advanceGeneration()`: rules.md 3장 규칙으로 1세대 진행한다.
- 스레드: 게임 진행과 같은 스레드에서만 부른다(UI 연결 방식은 #7에서 정함).
- `count(owner)`: 지금 살아 있는 `owner` 셀 수. 0 이상. 게임 진행은 시뮬레이션 중 매 세대 직후(판정)와 상태 조회(`score`)에서 부른다. 세는 방법(전체 순회 / 증분 / Visitor)은 #5에서 정한다([005](005-cell-count-and-judge.md) 2절).

---

# TwoPlayerGame

- 제공 도메인: 2인용 모드 / 사용 도메인: 2인용 UI(#7) / 담당자: Son2kwon
- 변경 이력:
  - 2026-11-03, 최초 작성 (#6 1차: 단계·차례·배치·패스·tick)
  - 2026-11-03, `score(Player)`, `result()`, `GameResult` 추가 (#6 2차: 승패 판정)

## 시그니처

```java
package com.holub.life.twoplayer;

public final class TwoPlayerGame {
  public TwoPlayerGame(TwoPlayerBoard board, GameSettings settings);

  // 입력 (004: 직접 호출 + 결과 값)
  public Response select(Player who, int row, int column);   // 임시 배치·이동
  public Response confirm(Player who);                         // 임시 배치 확정
  public Response pass(Player who);                            // 개입 단계만
  public boolean tick();                                       // 시뮬레이션 1세대, 아니면 무시하고 false

  // 상태 조회
  public PhaseKind phase();
  public Player currentPlayer();          // 시뮬레이션·종료 단계에서는 null
  public int remaining(Player player);    // 이번 배치 단계에서 남은 배치 개수
  public int round();                     // 초기 배치 중 0, 라운드 r의 시뮬레이션·개입 중 r
  public int generationInRound();         // 이번 라운드 시뮬레이션에서 진행한 세대 수
  public Optional<Coordinate> tentative(); // 현재 임시 배치 칸
  public int score(Player player);           // 지금 살아 있는 자기 셀 수 (= board.count)
  public Optional<GameResult> result();      // 종료 단계에서만 값이 있음
}

public enum PhaseKind { PLACEMENT, SIMULATION, INTERVENTION, GAME_OVER }
public enum Player { A, B; public Player opponent(); }
public enum Response { ACCEPTED, WRONG_PHASE, NOT_YOUR_TURN, OUT_OF_BOARD, CELL_NOT_EMPTY,
                       OPPONENT_HALF, NO_TENTATIVE_PLACEMENT, PASS_NOT_ALLOWED }
public final class GameSettings { /* N, K, M, R. 기본값 20, 20, 3, 5 */ }
public record Coordinate(int row, int column) {}
public record GameResult(Outcome outcome, EndReason reason, int scoreA, int scoreB) {}
public enum Outcome { A_WINS, B_WINS, DRAW }
public enum EndReason { WIPE_OUT, ROUNDS_COMPLETED }
```

## 계약

- 입력 메서드는 거부해도 예외를 던지지 않고 거부 이유를 돌려준다. 거부된 입력은 게임 상태를 바꾸지 않는다.
- 거부 이유 판단 순서: 단계 → 차례 → 보드 범위 → 진영(초기 배치만) → 빈 칸. 남은 개수가 0인 플레이어는 자동 패스로 차례가 오지 않으므로 별도 이유가 없다(`NOT_YOUR_TURN`).
- `tick()`은 시뮬레이션 단계에서만 보드를 1세대 진행하고 true를 돌려준다. 다른 단계에서는 아무것도 하지 않고 false.
- 선공·후공: 초기 배치는 A가 먼저 둔다. 이후 배치 단계(라운드 1 개입, 라운드 2 개입, …)마다 선공이 바뀐다(라운드 1 개입 선공 = B).
- 자동 패스: 개입 단계에서 차례가 온 플레이어의 남은 개수가 0이거나 `board.hasEmptyCell()`이 false면 그 차례는 자동으로 패스된다. 연속 패스 수에 들어간다.
- `GameSettings`: N·K·R은 1 이상, M은 0 이상. 어기면 생성자가 `IllegalArgumentException`.
- 스레드: 한 스레드에서만 부른다. 동기화하지 않는다.
- 판정(rules.md 6장, [005](005-cell-count-and-judge.md)): 시뮬레이션 중 매 세대 직후 한쪽이라도 0이면 즉시 종료(`WIPE_OUT`), 둘 다 0이면 무승부. R번째 라운드의 K번째 세대 뒤 전멸이 아니면 셀 수 비교(`ROUNDS_COMPLETED`), 같으면 무승부. 마지막 라운드가 아니어도 K번째 세대에 전멸하면 개입 없이 종료한다. 배치·개입 단계에서는 판정하지 않는다.
- `result()`는 종료 단계에서만 값이 있고, 이후 바뀌지 않는다.
