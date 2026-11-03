# 2인용 모드 인터페이스

- 근거: [003-game-phase.md](003-game-phase.md), [004-player-input.md](004-player-input.md)
- 공통 `Cell` 인터페이스 변경 제안은 core 쪽 문서에 있다: [../../docs/interfaces.md](../../docs/interfaces.md)

---

# TwoPlayerBoard

- 제공 도메인: 2인용 모드(구현은 #5 이후) / 사용 도메인: 2인용 게임 진행(`TwoPlayerGame`) / 담당자: Son2kwon
- 변경 이력:
  - 2026-11-03, 최초 작성 (#6). 셀 상태 회의(#5) 전이라 게임 진행이 보드에 묻는 것만 모았다.

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
}
```

## 계약

- `rows()`, `columns()`: 1 이상, 게임 중 바뀌지 않는다.
- `isEmpty(row, column)`: 그 칸이 `DEAD`이면 true. 사전조건: `0 <= row < rows()`, `0 <= column < columns()` (범위 검사는 호출하는 쪽이 먼저 한다).
- `hasEmptyCell()`: `isEmpty`가 true인 칸이 하나라도 있으면 true.
- `place(row, column, owner)`: 그 칸을 `owner`의 산 셀로 만든다. 사전조건: `isEmpty(row, column)`이 true. 게임 진행은 확정할 때만 부른다(임시 배치는 보드에 놓지 않는다).
- `advanceGeneration()`: rules.md 3장 규칙으로 1세대 진행한다.
- 스레드: 게임 진행과 같은 스레드에서만 부른다(UI 연결 방식은 #7에서 정함).
- 예정: 승패 판정(#6 2차)에서 `count(Player)`(살아 있는 자기 셀 수)를 추가한다.

---

# TwoPlayerGame

- 제공 도메인: 2인용 모드 / 사용 도메인: 2인용 UI(#7) / 담당자: Son2kwon
- 변경 이력:
  - 2026-11-03, 최초 작성 (#6 1차: 단계·차례·배치·패스·tick)

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
  public Optional<Point> tentative();     // 현재 임시 배치 칸 (x = 열, y = 행)
}

public enum PhaseKind { PLACEMENT, SIMULATION, INTERVENTION, GAME_OVER }
public enum Player { A, B; public Player opponent(); }
public enum Response { ACCEPTED, WRONG_PHASE, NOT_YOUR_TURN, OUT_OF_BOARD, CELL_NOT_EMPTY,
                       OPPONENT_HALF, NO_PLACEMENTS_LEFT, NO_TENTATIVE_PLACEMENT, PASS_NOT_ALLOWED }
public final class GameSettings { /* N, K, M, R. 기본값 20, 20, 3, 5 */ }
```

## 계약

- 입력 메서드는 거부해도 예외를 던지지 않고 거부 이유를 돌려준다. 거부된 입력은 게임 상태를 바꾸지 않는다.
- `tick()`은 시뮬레이션 단계에서만 보드를 1세대 진행하고 true를 돌려준다. 다른 단계에서는 아무것도 하지 않고 false.
- 선공·후공: 초기 배치는 A가 먼저 둔다. 이후 배치 단계(라운드 1 개입, 라운드 2 개입, …)마다 선공이 바뀐다(라운드 1 개입 선공 = B).
- 자동 패스: 개입 단계에서 차례가 온 플레이어의 남은 개수가 0이거나 `board.hasEmptyCell()`이 false면 그 차례는 자동으로 패스된다. 연속 패스 수에 들어간다.
- `GameSettings`: N·K·R은 1 이상, M은 0 이상. 어기면 생성자가 `IllegalArgumentException`.
- 스레드: 한 스레드에서만 부른다. 동기화하지 않는다.
- 이번 범위 밖: 시뮬레이션 중 전멸 검사와 승패 결과(#6 2차).
