# 2인용 모드 테스트 케이스

- 근거: [rules.md](rules.md) (2인용 Life Game 규칙 명세). 아래에서 "명세 N장"은 이 문서를 뜻한다.
- 상태: TP-F·TP-P는 #6 1차, TP-E는 #6 2차에서 테스트 작성(아래 각 장 참고). TP-R·TP-B·TP-I는 #5에서 [006](006-two-player-cell.md) 설계(F) 기준으로 정의하고 테스트를 작성했다(37개, 골격 단계에서 모두 실패 → 구현 후 통과). TP-U는 #7에서 [007](007-two-player-ui.md) 기준으로 다시 정의하고 테스트를 작성했다(5장, 17개, `TwoPlayerUiTest`. 골격 단계에서 17개 실패 → 구현 후 통과).
- 공통 전제
  - 좌표는 (행, 열)이고 0부터 센다. 보드 밖은 항상 `DEAD`다.
  - 격자 표기: `A`·`B`는 각 플레이어의 살아 있는 셀, `.`은 죽은 셀이다. 적지 않은 아래쪽 행은 모두 `.`이다.
  - 모든 테스트는 Swing 없이 모델만으로 돌린다(UI 케이스 제외). 세대 진행은 Clock 대신 수동 tick으로 하고, N·K·M·R은 작은 값을 주입한다(AGENTS.md 3-1).
  - 기대값 중 명세 7장 예시는 별도 Python 시뮬레이터로 다시 돌려 확인했다(아래 "기대값 검증 기록").

## 1. 세대 전이 규칙과 실제 보드 (명세 3장, #5)

대상: [006](006-two-player-cell.md)의 F. `twoplayer` 안의 2인용 셀(`TwoPlayerResident`)과 `Neighborhood`로 만든 `TwoPlayerBoard` 구현 `NeighborhoodTwoPlayerBoard`. 별도 표시가 없으면 보드는 64×64(`Neighborhood(8, Neighborhood(8, 2인용 셀))`, `Life.java`와 같은 구성)이고, A 진영은 0~31열, B 진영은 32~63열이다.

**기대값 출처 표기** (AGENTS.md 2-1)

| 표기 | 뜻 |
| --- | --- |
| [명세 7-n] | rules.md 7장 예시 n의 값(Python 시뮬레이터로 검증된 값, 아래 "기대값 검증 기록") |
| [명세 3] | rules.md 3장 전이 표를 그 칸에 직접 적용한 값 |
| [CCB] | `CellCandidateBenchmark`의 F 프로토타입(`CandidateF`, 64×64)으로 계산한 값 (2026-11-15, 아래 "기대값 검증 기록") |
| [계약] | 이 문서에서 정하는 `TwoPlayerBoard` 계약 값. 구현 PR에서 [interfaces.md](interfaces.md)에 반영 |

명세 예시는 5×5·6×10·10×10·16×16 보드지만, 같은 좌표에 두고 64×64에서 돌려도 결과가 같았다([CCB], 예시 1~5 전부). 그래서 TP-R은 모두 64×64 기본 보드에서 확인한다(`Neighborhood`는 정사각형이라 6×10은 만들 수 없다).

### 1-1. 세대 전이 규칙

클래스: `TwoPlayerRuleTest`

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 출처 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- | --- |
| TP-R-01 | 정상 | 이웃 3개 중 A가 2개면 A로 태어난다 (명세 예시 1) | A(1,1), A(2,2), B(3,1) | 1세대 뒤 살아 있는 칸은 A(2,1), A(2,2)뿐. A=2, B=0 | [명세 7-1] [CCB] | `TwoPlayerRuleTest#bornAsMajorityColorA` |
| TP-R-02 | 정상 | 이웃 3개 중 B가 2개면 B로 태어난다 (TP-R-01의 색 반전) | B(1,1), B(2,2), A(3,1) | 1세대 뒤 B(2,1), B(2,2)뿐. A=0, B=2 | [명세 3] [CCB] | `TwoPlayerRuleTest#bornAsMajorityColorB` |
| TP-R-03 | 정상 | 상대 셀이 몰리면 내 셀이 과밀로 죽는다 (명세 예시 2) | A(1,1), B(1,2), A(1,3), A(2,2), B(3,2) | 1세대 뒤 (2,2) `DEAD`, (0,2)에 A 탄생. 살아 있는 칸은 A(0,2), A(1,1), B(1,2), A(1,3). A=3, B=1 | [명세 7-2] [CCB] | `TwoPlayerRuleTest#opponentNeighborsCauseOvercrowding` |
| TP-R-04 | 정상 | 살아 있는 셀은 이웃이 모두 상대 색이어도 자기 색을 유지한다 | B(1,1), A(2,2), B(3,3) | 1세대 뒤 A(2,2)만 살아남음(이웃 2). A=1, B=0 | [명세 3] [CCB] | `TwoPlayerRuleTest#survivingCellKeepsItsColor` |
| TP-R-05 | 정상 | 같은 색 3개로 태어난 셀은 그 색이다 | A(1,1), A(1,2), A(1,3) | 1세대 뒤 A(0,2), A(1,2), A(2,2)뿐 (세로 blinker). A=3 | [명세 3] [CCB] | `TwoPlayerRuleTest#bornFromThreeSameColor` |
| TP-R-06 | 정상 | 색을 가진 진동자도 주기마다 같은 색으로 돌아온다 | A(10,10), A(10,11), A(10,12) | 세대 1: A(9,11), A(10,11), A(11,11). 세대 2: 세대 0과 칸·색이 모두 같음 | [명세 3] [CCB] | `TwoPlayerRuleTest#coloredBlinkerKeepsColorEachPeriod` |
| TP-R-07 | 경계 | 이웃이 1개 이하면 색과 상관없이 고립사 | TP-R-01과 같음. B(3,1)의 이웃은 A(2,2) 1개 | 1세대 뒤 (3,1) `DEAD` | [명세 7-1] [CCB] | `TwoPlayerRuleTest#isolatedCellDies` |
| TP-R-08 | 경계 | 죽은 칸은 이웃이 4개면 태어나지 않는다 | A(1,1), A(1,3), B(3,1), B(3,3). (2,2)는 `DEAD`이고 이웃 4개 | 1세대 뒤 (2,2) `DEAD`. 네 셀도 모두 고립사, A=0, B=0 | [명세 3] [CCB] | `TwoPlayerRuleTest#noBirthWithFourNeighbors` |
| TP-R-09 | 경계 | 보드 모서리에서는 바깥을 `DEAD`로 본다 | A(0,1), A(1,0), A(1,1) | 1세대 뒤 A(0,0), A(0,1), A(1,0), A(1,1) (2×2 block). A=4 | [명세 3] [CCB] | `TwoPlayerRuleTest#cornerTreatsOutsideAsDead` |
| TP-R-10 | 경계 | 하위 `Neighborhood` 경계(8칸 단위)를 넘는 칸에서도 탄생 색이 맞다 | A(7,7), A(7,8), B(8,7). (8,8)은 네 블록이 만나는 칸 | 1세대 뒤 (8,8)이 A. 살아 있는 칸은 A(7,7), A(7,8), B(8,7), A(8,8). A=3, B=1 | [명세 3] [CCB] | `TwoPlayerRuleTest#birthAcrossNeighborhoodBoundary` |
| TP-R-11 | 경계 | 한 번도 활성화된 적 없는 `Neighborhood`도 이웃 블록 가장자리의 변화 때문에 다시 계산된다 (활성 블록 최적화 회귀) | 세로 blinker A(9,7), A(10,7), A(11,7). 7열은 블록 (1,0)의 동쪽 가장자리이고, (10,8)은 아무것도 놓지 않은 블록 (1,1)에 있다 | 세대 1·3·5: A(10,6), A(10,7), A(10,8) (블록 (1,1)의 (10,8)에 A 탄생). 세대 2·4·6: A(9,7), A(10,7), A(11,7), (10,8) `DEAD` | [명세 3] [CCB] | `TwoPlayerRuleTest#quietNeighborhoodWakesOnEdgeBirth` |
| TP-R-12 | 정상 | 서로 닿지 않는 진동자와 정물은 영원히 같은 상태를 반복한다 (명세 예시 3) | A(2,1), A(2,2), A(2,3), B block (2,6)·(2,7)·(3,6)·(3,7) | 세대 0~6 매 세대 A=3, B=4. 세대 2의 보드가 세대 0과 같음 | [명세 7-3] [CCB] | `TwoPlayerRuleTest#disjointPatternsRepeatForever` |
| TP-R-13 | 정상 | glider와 block이 부딪히면 같은 세대에 둘 다 전멸한다 (명세 예시 4) | A glider (0,1)·(1,2)·(2,0)·(2,1)·(2,2), B block (5,5)·(5,6)·(6,5)·(6,6) | A:B가 세대 0~6 5:4, 7 6:5, 8 3:3, 9 1:2, 10 0:0 | [명세 7-4] [CCB] | `TwoPlayerRuleTest#gliderHitsBlockBothVanish` |
| TP-R-14 | 정상 | 섞인 이웃에서 다수색으로 태어나는 셀이 충돌 지점에 생긴다 (명세 예시 4) | TP-R-13과 같음 | 세대 7의 (5,4)가 B | [명세 7-4] [CCB] | `TwoPlayerRuleTest#mixedNeighborsBirthAtCollision` |
| TP-R-15 | 정상 | 색을 지우면 클래식 B3/S23과 살아 있는 칸이 항상 같다 (불변조건) | 같은 칸을 2인용 보드에는 `place`, 클래식 보드 `CompositeBoard(new Neighborhood(8, new Neighborhood(8, new Resident())))`에는 `toggle`로 둔다. (a) TP-R-01·03·12·13의 배치, 각 10세대. (b) 64×64 무작위 보드: `new Random(42)`, 행 우선으로 칸마다 `nextDouble() < 0.3`이면 살림, 0~31열 A·32~63열 B, 200세대 | 모든 세대에서 2인용 보드의 살아 있는 칸 집합 = 클래식 보드의 살아 있는 칸 집합 | [CCB] (`checkEquivalence`, (b)와 같은 보드로 200세대 일치) | `TwoPlayerRuleTest#sameAliveCellsAsClassicWhenColorIgnored` |
| TP-R-16 | 정상 | 셀 1개 저격에 대응이 없으면 block이 무너진다 (명세 예시 5) | B block (7,7)·(7,8)·(8,7)·(8,8), A(8,9) | A:B가 세대 0~4 1:4, 1:4, 1:2, 0:2, 0:0 | [명세 7-5] [CCB] | `TwoPlayerRuleTest#snipeWithoutResponse` |
| TP-R-17 | 정상 | (5,8) 대응을 두면 B가 살아남는다 (명세 예시 5) | TP-R-16 + B(5,8) | A=0은 세대 8부터. B는 세대 8~17이 16, 32, 24, 40, 32, 40, 44, 56, 20, 24이고 세대 17~30 내내 24. 세대 30의 살아 있는 칸은 B 24개뿐: (1,7), (2,6), (2,8), (3,6), (3,8), (4,7), (6,2), (6,3), (6,11), (6,12), (7,1), (7,4), (7,10), (7,13), (8,2), (8,3), (8,11), (8,12), (10,7), (11,6), (11,8), (12,6), (12,8), (13,7) | [명세 7-5] (세대 17 고정·세대 30 그림) [CCB] (칸 목록) | `TwoPlayerRuleTest#snipeWithResponseAt5_8` |
| TP-R-18 | 예외 | 2인용 셀을 추가해도 클래식 모드 동작은 바뀌지 않는다 (강령 1) | `twoplayer` 밖의 기존 테스트 전체 (2026-11-15 기준 144개 = 전체 186개 − 2인용 42개) | 수정 없이 모두 통과 | 기존 테스트 | (기존 클래식·core 테스트 클래스) |

TP-R-16·17은 전이 규칙만 확인한다(승패 판정 없이 30세대 진행). 같은 보드를 게임 진행에 넣으면 A가 먼저 0개가 되어 그 세대에 종료된다(6장). 명세 예시 5는 보드의 다른 셀을 생략한 국소 예시이기 때문이다.

TP-R-11은 정의를 구체화했다. 이전 정의("한 블록 가장자리 바로 옆에 탄생 조건을 만듦")는 입력이 정해지지 않았다. 블록 (1,1)은 `place`로도 `toggle`로도 건드리지 않으므로, 블록 (1,0)의 동쪽 가장자리 변화(`Neighborhood.java:264-283`)가 없으면 계산되지 않는다.

### 1-2. `TwoPlayerBoard` 계약

클래스: `NeighborhoodTwoPlayerBoardTest`. TP-R이 다루지 않는 `rows`·`columns`, `isEmpty`, `hasEmptyCell`, `place`, `count`와 계약 위반을 확인한다.

계약 위반 시 동작: [interfaces.md](interfaces.md)는 "범위 검사는 호출하는 쪽이 먼저 한다", "`place`의 사전조건: `isEmpty`가 true"까지만 정하고, 어겼을 때 무엇을 하는지는 정하지 않았다. 아래 TP-B 예외 케이스는 그 동작을 **[계약]으로 새로 정한다.** 구현 PR에서 interfaces.md에 함께 반영한다.

- 범위 밖 좌표: `IndexOutOfBoundsException` (`CompositeBoard.toggle`이 `Objects.checkIndex`를 쓰는 것과 같은 방식, `model/CompositeBoard.java:36-37`)
- 살아 있는 칸에 `place`: `IllegalArgumentException`, 보드는 바뀌지 않음. 막지 않으면 `Neighborhood.toggle`이 그 칸을 뒤집어 **죽인다** ([CCB]: 프로토타입에서 A(10,10)에 B를 `place`하면 (10,10) `DEAD`, A=0, B=0)
- `owner`가 `null`: `NullPointerException`. 막지 않으면 `count(null)`은 `DEAD` 칸 수를 센다(`CandidateF.count`가 소유자를 `==`로 비교하므로)
- 게임 진행(`TwoPlayerGame`)은 범위·빈 칸을 먼저 확인하므로(TP-P-05·12), 정상 진행에서는 이 예외가 나지 않는다

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 출처 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- | --- |
| TP-B-01 | 정상 | 기본 보드는 64×64다 | 새 보드 | `rows()` = 64, `columns()` = 64 | [계약] (006, `Life.java:19`와 같은 구성) | `NeighborhoodTwoPlayerBoardTest#defaultBoardIs64By64` |
| TP-B-02 | 정상 | 새 보드는 모든 칸이 비어 있다 | 새 보드 | 4,096칸 모두 `isEmpty` = true, `count(A)` = 0, `count(B)` = 0 | [계약] | `NeighborhoodTwoPlayerBoardTest#newBoardIsEmpty` |
| TP-B-03 | 정상 | `place`한 칸만 비지 않게 되고 그 소유자 수가 1 늘어난다 | 새 보드, `place(10, 10, A)` | `isEmpty(10,10)` = false, 이웃 8칸은 true. A=1, B=0 | [계약] | `NeighborhoodTwoPlayerBoardTest#placeFillsOnlyThatCell` |
| TP-B-04 | 정상 | B도 같은 방식으로 놓인다 | 새 보드, `place(10, 50, B)` | `isEmpty(10,50)` = false. A=0, B=1 | [계약] | `NeighborhoodTwoPlayerBoardTest#placeCountsForOwnerB` |
| TP-B-05 | 정상 | 보드는 진영을 검사하지 않는다 (진영 제한은 게임 진행의 일, TP-P-06·08) | 새 보드, `place(10, 40, A)` (B 진영) | 예외 없음. `isEmpty(10,40)` = false, A=1 | [계약] | `NeighborhoodTwoPlayerBoardTest#boardDoesNotCheckHalves` |
| TP-B-06 | 정상 | 세대 진행으로 생기고 죽은 칸이 `isEmpty`에 반영된다 | TP-R-01 배치, `advanceGeneration()` 1회 | `isEmpty(2,1)` = false (탄생), `isEmpty(3,1)` = true (고립사) | [명세 7-1] [CCB] | `NeighborhoodTwoPlayerBoardTest#isEmptyReflectsBirthAndDeath` |
| TP-B-07 | 정상 | 정체한 보드에 `place`한 셀도 다음 세대 계산에 들어간다 | 새 보드, `advanceGeneration()` 5회 뒤 `place` A(20,20), A(20,21), A(20,22), `advanceGeneration()` 1회 | 살아 있는 칸은 A(19,21), A(20,21), A(21,21)뿐. A=3 | [명세 3] [CCB] | `NeighborhoodTwoPlayerBoardTest#placeOnQuietBoardJoinsNextGeneration` |
| TP-B-08 | 경계 | 네 모서리 칸에도 놓을 수 있다 | 새 보드, `place` A(0,0), A(63,0), B(0,63), B(63,63) | 네 칸 모두 `isEmpty` = false. A=2, B=2 | [계약] | `NeighborhoodTwoPlayerBoardTest#placeOnFourCorners` |
| TP-B-09 | 정상 | 빈 칸이 있으면 `hasEmptyCell`은 true | 새 보드 | `hasEmptyCell()` = true | [계약] | `NeighborhoodTwoPlayerBoardTest#hasEmptyCellOnNewBoard` |
| TP-B-10 | 경계 | 빈 칸이 딱 하나 남아도 true, 다 차면 false | 0~31열 A, 32~63열 B로 (63,63)만 빼고 4,095칸 `place` → 확인 → `place(63, 63, B)` → 확인 | 4,095칸: `hasEmptyCell()` = true, A=2,048, B=2,047. 4,096칸: `hasEmptyCell()` = false, A=2,048, B=2,048 | [계약] [CCB] (가득 찬 보드 2048:2048) | `NeighborhoodTwoPlayerBoardTest#hasEmptyCellUntilLastCellFilled` |
| TP-B-11 | 경계 | 가득 찬 보드는 1세대 뒤 네 모서리만 남는다 (모서리 이웃 3, 가장자리 5, 안쪽 8) | TP-B-10의 가득 찬 보드, `advanceGeneration()` 2회 | 세대 1: 살아 있는 칸은 A(0,0), A(63,0), B(0,63), B(63,63)뿐, A=2, B=2, `hasEmptyCell()` = true. 세대 2: A=0, B=0 | [명세 3] [CCB] | `NeighborhoodTwoPlayerBoardTest#fullBoardLeavesOnlyCorners` |
| TP-B-12 | 경계 | 빈 보드의 세대 진행은 아무것도 바꾸지 않는다 | 새 보드, `advanceGeneration()` 1회 | 예외 없음. A=0, B=0, `hasEmptyCell()` = true | [명세 3] [CCB] | `NeighborhoodTwoPlayerBoardTest#emptyBoardStaysEmpty` |
| TP-B-13 | 예외 | 범위 밖 좌표의 `isEmpty`는 예외를 던진다 | 새 보드, `isEmpty`(-1,0), (0,-1), (64,0), (0,64) | 각각 `IndexOutOfBoundsException` | [계약] | `NeighborhoodTwoPlayerBoardTest#isEmptyRejectsOutOfBoard` |
| TP-B-14 | 예외 | 범위 밖 좌표의 `place`는 예외를 던지고 보드를 바꾸지 않는다 | 새 보드, `place(A)`를 (-1,0), (0,-1), (64,0), (0,64)에 | 각각 `IndexOutOfBoundsException`. 이후 A=0, B=0 | [계약] | `NeighborhoodTwoPlayerBoardTest#placeRejectsOutOfBoard` |
| TP-B-15 | 예외 | 살아 있는 칸에는 `place`할 수 없고, 거부해도 그 셀은 그대로다 | `place(10, 10, A)` 뒤 `place(10, 10, A)`, `place(10, 10, B)` | 둘 다 `IllegalArgumentException`. 이후 (10,10)은 A 그대로: `isEmpty(10,10)` = false, A=1, B=0 | [계약] ([CCB]: 막지 않으면 0:0) | `NeighborhoodTwoPlayerBoardTest#placeOnLiveCellIsRejectedAndKeepsCell` |
| TP-B-16 | 예외 | 소유자 없이 `place`할 수 없다 | 새 보드, `place(10, 10, null)` | `NullPointerException`. 이후 `isEmpty(10,10)` = true, A=0, B=0 | [계약] | `NeighborhoodTwoPlayerBoardTest#placeRejectsNullOwner` |
| TP-B-17 | 예외 | 소유자 없이 `count`할 수 없다 (`DEAD` 칸 수를 잘못 돌려주지 않음) | 새 보드, `count(null)` | `NullPointerException` | [계약] | `NeighborhoodTwoPlayerBoardTest#countRejectsNullOwner` |

### 1-3. 실제 보드로 게임 진행 다시 확인

클래스: `TwoPlayerGameOnRealBoardTest`. `TwoPlayerGame`에 `StubBoard` 대신 `NeighborhoodTwoPlayerBoard`를 넣고, 2장·4장의 대표 시나리오를 다시 돌린다. 초기 배치는 `select` → `confirm`을 A, B 번갈아 하며, 아래 "배치"에 적은 순서대로 둔다(각자의 목록에서 앞에서부터).

**고른 이유**: 대역이 실제 보드와 다른 곳은 두 가지뿐이다. (1) `advanceGeneration()`이 보드를 바꾸지 않는다(2장 머리말), (2) 셀 수를 주입한다(4장 머리말). 그래서 (1)에 기대는 유일한 게임 진행 케이스인 TP-F-13, (2)의 종료 경로 두 가지(`WIPE_OUT`, `ROUNDS_COMPLETED`)를 대역 값의 출처였던 명세 예시로 고른 TP-E-02·03을 다시 돌린다. 나머지 TP-F·TP-P는 보드에 `isEmpty`·`hasEmptyCell`·`place`만 묻고, 이 셋은 1-2장이 실제 보드로 확인한다. TP-E-01·04·06·09는 TP-E-02·03과 같은 판정 경로라 빼고, TP-F-10(빈칸 없음 자동 패스)은 게임 진행으로 4,096칸을 채워야 해서 빼고 `hasEmptyCell` = false를 TP-B-10에서 확인한다.

명세 예시 3·4는 B의 block이 A 진영(32열 왼쪽)에 있어 초기 배치 규칙(TP-P-06)으로는 둘 수 없다. 그래서 TP-I-02는 예시 4 전체를 오른쪽으로 27열 옮기고, TP-I-03은 B의 block만 40열로 옮겼다. 두 플레이어의 배치 개수(N)를 맞추려고 넣은 고립 셀 1개는 세대 1에 죽는다. 옮긴 배치의 세대별 셀 수는 [CCB]로 계산했고, 세대 1부터 명세 값과 같다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 출처 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- | --- |
| TP-I-01 | 경계 | 실제 정물 보드에서도 K세대를 모두 진행한 뒤 개입으로 간다 (TP-F-13) | N=4, K=3, M=2, R=2. 배치 A: (10,10), (10,11), (11,10), (11,11) / B: (10,50), (10,51), (11,50), (11,51) (block 2개). `tick()` 3회 | `tick()` 2회 뒤 단계 = 시뮬레이션, `generationInRound()` = 2. 3회 뒤 단계 = 개입, `round()` = 1, `score(A)` = 4, `score(B)` = 4, 보드가 세대 0과 같음 | [명세 3] (block 정물) [CCB] (세대 1~3 4:4) | `TwoPlayerGameOnRealBoardTest#stableRealBoardStillRunsAllKGenerations` |
| TP-I-02 | 정상 | 실제 보드에서 같은 세대 동시 전멸이 무승부로 끝난다 (TP-E-02, 명세 예시 4) | N=5, K=20, M=2, R=1. 배치 A: glider (0,28), (1,29), (2,27), (2,28), (2,29) / B: block (5,32), (5,33), (6,32), (6,33) + 고립 셀 (40,60). 게임이 끝날 때까지 `tick()` | `score` A:B가 세대 0 5:5, 1~6 5:4, 7 6:5, 8 3:3, 9 1:2, 10 0:0. 10번째 `tick()` 뒤 단계 = 종료, `result()` = `GameResult(DRAW, WIPE_OUT, 0, 0)`. 11번째 `tick()`은 false | [명세 7-4] [CCB] (옮긴 배치) | `TwoPlayerGameOnRealBoardTest#simultaneousWipeOutIsDrawOnRealBoard` |
| TP-I-03 | 정상 | 실제 보드에서 R라운드 종료 시 셀이 많은 쪽이 이긴다 (TP-E-03, 명세 예시 3) | N=4, K=4, M=2, R=1. 배치 A: blinker (2,1), (2,2), (2,3) + 고립 셀 (40,10) / B: block (2,40), (2,41), (3,40), (3,41). `tick()` 4회 | `score` A:B가 세대 0 4:4, 1~4 3:4. 4번째 `tick()` 뒤 단계 = 종료, `result()` = `GameResult(B_WINS, ROUNDS_COMPLETED, 3, 4)` | [명세 7-3] [CCB] (옮긴 배치) | `TwoPlayerGameOnRealBoardTest#moreCellsWinsAfterLastRoundOnRealBoard` |

## 2. 게임 진행 (명세 4장, #6)

클래스: `TwoPlayerGameFlowTest` (#6 1차). 별도 표시가 없으면 N=2, K=3, M=2, R=2를 주입한다. 보드는 셀 상태 회의(#5) 전이라 `TwoPlayerBoard` 대역(`StubBoard`, 64×64)을 쓴다. 대역은 확정 배치된 칸을 기억해 `isEmpty`에 반영하고, `advanceGeneration()` 호출 수를 센다. 대역의 `advanceGeneration()`은 보드를 바꾸지 않으므로 TP-F-13의 "정물 보드"에 해당한다(실제 보드로 다시 확인하는 케이스: 1-3장 TP-I-01)([003](003-game-phase.md) "셀 상태와의 분리").

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| TP-F-01 | 정상 | 새 게임은 초기 배치 단계에서 A 차례로 시작한다 | 새 게임 | 단계 = 초기 배치, 차례 = A, 남은 개수 A=N·B=N | `TwoPlayerGameFlowTest#startsInPlacementWithA` |
| TP-F-02 | 정상 | 초기 배치는 1개씩 번갈아 둔다 | A 확정 → B 확정 → A 확정 | 차례가 A → B → A → B 순서로 바뀜 | `#placementAlternatesOneByOne` |
| TP-F-03 | 정상 | 양쪽이 N개씩 두면 시뮬레이션 단계로 넘어간다 | A·B 각 2개 확정 | 단계 = 시뮬레이션, 세대 수 0 | `#placementEndsAfterNEach` |
| TP-F-04 | 정상 | 시뮬레이션은 K세대 뒤 개입 단계로 넘어간다 | 시뮬레이션 단계, 수동 tick 3회 | 2회까지는 시뮬레이션, 3회 뒤 단계 = 개입 | `#simulationEndsAfterKGenerations` |
| TP-F-05 | 정상 | 개입 단계를 시작하면 각자 M개를 받는다 | 개입 단계 진입 | 남은 개수 A=M·B=M | `#interventionGrantsMEach` |
| TP-F-06 | 정상 | 두 사람이 연달아 패스하면 개입이 끝난다 | 개입: 선공 패스 → 후공 패스 | 다음 라운드 시뮬레이션 단계로 넘어감 | `#twoConsecutivePassesEndIntervention` |
| TP-F-07 | 경계 | 패스 사이에 배치가 끼면 "연달아"가 아니다 | 개입: 패스 → 배치 → 패스 | 아직 개입 단계, 차례가 넘어감 | `#passPlacePassDoesNotEnd` |
| TP-F-08 | 정상 | 선공과 후공은 초기 배치부터 이어서 번갈아 간다 | R=3, 라운드 1·2의 개입 시작 | 라운드 1 개입 선공 = B(초기 배치 후공), 라운드 2 개입 선공 = A | `#interventionFirstPlayerAlternates` |
| TP-F-09 | 경계 | 개수가 0인 플레이어는 자동으로 패스한다 | 개입: A가 M개를 모두 씀 | A 차례가 오면 자동 패스 처리, B가 패스하면 개입 종료 | `#autoPassWhenNoPlacementsLeft` |
| TP-F-10 | 경계 | 둘 빈칸이 없는 플레이어는 자동으로 패스한다 | 빈칸이 없는 작은 보드에서 개입 진입 | 양쪽 자동 패스, 개입 즉시 종료 | `#autoPassWhenNoEmptyCell` |
| TP-F-11 | 경계 | 쓰지 않은 개수는 다음 라운드로 넘어가지 않는다 | 라운드 1 개입에서 1개만 쓰고 패스 | 라운드 2 개입 시작 시 남은 개수 = M (M+1 아님) | `#unusedPlacementsAreDiscarded` |
| TP-F-12 | 정상 | R번째 라운드의 시뮬레이션 뒤에는 개입 없이 종료한다 | R=2, 라운드 2 시뮬레이션 K세대 완료 | 단계 = 종료 | `#gameEndsAfterLastSimulation` |
| TP-F-13 | 경계 | 보드가 정체돼도 K세대를 모두 진행한다 | 정물(block)만 있는 보드, K=3 | 세대 수가 3이 된 뒤에야 개입 단계 | `#stableBoardStillRunsAllKGenerations` |
| TP-F-14 | 정상 | 시뮬레이션 단계의 수동 Tick은 1세대로 K에 포함된다 | 시뮬레이션, 수동 Tick 1회 + 자동 진행 | 합계 K세대에서 개입으로 넘어감 | `#manualTickCountsTowardK` |
| TP-F-15 | 예외 | 시뮬레이션이 아닌 단계의 Tick은 무시된다 | 초기 배치·개입 단계에서 Tick | 보드·세대 수 변화 없음 | `#tickIgnoredOutsideSimulation` |
| TP-F-16 | 정상 | 설정을 주입하지 않으면 기본값을 쓴다 | 기본 생성 | N=20, K=20, M=3, R=5 | `#defaultSettings` |
| TP-F-17 | 예외 | 잘못된 설정값은 거부한다 | N·K·R ≤ 0 또는 M < 0 | `IllegalArgumentException` | `#rejectsInvalidSettings` |

TP-F-08 규칙(명세 4장, 2026-11-03 확정): 선공과 후공은 초기 배치부터 이어서 번갈아 간다. 초기 배치의 후공이 B이므로 라운드 1 개입의 선공은 B이고, 라운드 2는 A다. TP-F-17은 명세에 없는 방어 규칙이다(설정값으로 뺀다는 4장 결정에서 나옴).

## 3. 배치 규칙 (명세 5장, #6)

클래스: `PlacementRuleTest` (#6 1차, `StubBoard` 사용). 보드는 64×64이고 A 진영은 0~31열, B 진영은 32~63열이다. "살아 있는 칸"은 대역에서 `isEmpty`가 false인 칸이다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| TP-P-01 | 정상 | 자기 차례에 자기 진영 빈칸을 클릭하면 임시 배치된다 | 초기 배치, A가 (10,10) 클릭 | (10,10)에 A 임시 배치, 차례 그대로 A | `PlacementRuleTest#clickPlacesTentativeCell` |
| TP-P-02 | 정상 | 확정 전에 다른 빈칸을 클릭하면 임시 배치가 옮겨진다 | TP-P-01 뒤 A가 (11,11) 클릭 | (10,10) `DEAD`, (11,11) 임시 A. 임시 배치는 항상 1개 | `#clickAnotherCellMovesTentative` |
| TP-P-03 | 정상 | 확정하면 셀이 고정되고 차례가 넘어가며 개수가 줄어든다 | TP-P-01 뒤 확정 | (10,10) A, 차례 = B, A 남은 개수 N-1 | `#confirmFixesCellAndPassesTurn` |
| TP-P-04 | 예외 | 확정한 셀은 취소할 수 없다 | TP-P-03 뒤 다시 A 차례에 (10,10) 클릭 | 거부, 이유 = 살아 있는 칸 | `#confirmedCellCannotBeUndone` |
| TP-P-05 | 예외 | 자기·상대의 살아 있는 칸에는 둘 수 없다 | A 셀·B 셀이 있는 칸 클릭 | 둘 다 거부, 이유 = 살아 있는 칸, 보드 변화 없음 | `#cannotPlaceOnLiveCell` |
| TP-P-06 | 예외 | 초기 배치에서는 상대 진영에 둘 수 없다 | 초기 배치, A가 (10,40) 클릭 | 거부, 이유 = 상대 진영 | `#placementRejectsOpponentHalf` |
| TP-P-07 | 경계 | 진영 경계 바로 양쪽 열 | 초기 배치, A가 31열·32열 클릭 / B가 32열·31열 클릭 | A: 31열 허용·32열 거부, B: 32열 허용·31열 거부 | `#halfBoundaryColumns` |
| TP-P-08 | 정상 | 개입 단계에서는 상대 진영에도 둘 수 있다 | 개입, A가 (10,40) 클릭 후 확정 | (10,40) A | `#interventionAllowsOpponentHalf` |
| TP-P-09 | 예외 | 차례가 아닌 플레이어는 둘 수 없다 | A 차례에 B 배치 요청 | 거부, 이유 = 차례 아님 | `#rejectsPlayerOutOfTurn` |
| TP-P-10 | 예외 | 남은 개수가 0인 플레이어는 둘 수 없다 | 개입, A가 M개를 모두 쓴 뒤(자동 패스로 B 차례) A가 배치 요청 | 거부, 이유 = 차례 아님 | `#rejectsWhenNoPlacementsLeft` |
| TP-P-11 | 예외 | 시뮬레이션·종료 단계에서는 배치 입력을 받지 않는다 | 시뮬레이션·종료 단계에서 클릭 | 거부, 이유 = 배치 단계 아님, 보드 변화 없음 | `#rejectsPlacementOutsidePlacementPhases` |
| TP-P-12 | 예외 | 보드 밖 좌표는 거부한다 | (-1,0), (0,64), (64,0) | 거부, 예외로 죽지 않음 | `#rejectsOutOfBoardCoordinate` |
| TP-P-13 | 예외 | 임시 배치 없이 확정할 수 없다 | 아무것도 클릭하지 않고 확정 | 거부, 차례 그대로 | `#confirmWithoutTentativeIsRejected` |
| TP-P-14 | 예외 | 초기 배치에서는 패스할 수 없다 | 초기 배치에서 패스 | 거부, 차례 그대로 | `#passRejectedDuringPlacement` |
| TP-P-15 | 경계 | 초기 배치의 마지막 수로 경계 건너편 상대 셀 옆에 두는 것은 허용된다(의도된 위험) | A가 (10,31)에 둔 뒤, B의 N번째 수로 (10,32) | 허용 | `#lastPlacementNextToBorderIsAllowed` |

TP-P-13 규칙(명세 5장, 2026-11-03 확정): 임시 배치 없이 확정하면 거부한다. 차례는 넘어가지 않는다.

TP-P-10: 남은 개수가 0인 플레이어는 자동 패스(TP-F-09) 때문에 차례가 오지 않는다. 그래서 그 플레이어의 배치 요청은 "차례 아님"으로 거부된다. 별도의 "남은 개수 없음" 거부는 생길 수 없어서 두지 않았다(#6 1차에서 정의 수정).

거부 이유를 판단하는 순서: 단계(배치 단계인가) → 차례 → 보드 범위 → 진영(초기 배치만) → 빈 칸인가.

## 4. 종료 조건과 승패 판정 (명세 6장, #6 2차)

클래스: `GameJudgeTest` (#6 2차). 보드는 `StubBoard`를 쓰고, 세대마다의 셀 수를 대역에 주입한다(세대 진행 1번마다 다음 값). 주입하는 값은 명세 7장 예시의 실제 셀 수(시뮬레이터로 확인한 값)다. 같은 예시를 실제 보드로 다시 확인하는 케이스: 1-3장 TP-I-02(TP-E-02), TP-I-03(TP-E-03). 판정 규칙: [005](005-cell-count-and-judge.md). 별도 표시가 없으면 N=2, M=2를 주입한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| TP-E-01 | 정상 | 한쪽이 전멸하면 K세대를 기다리지 않고 바로 끝난다 | K=10, R=1, 세대 1의 셀 수 A=0·B=4 (A 고립 셀 1개, B block) | 세대 1에 종료, B 승, 이유 = 전멸, 세대 수 1 | `GameJudgeTest#oneSideWipedOutEndsImmediately` |
| TP-E-02 | 정상 | 같은 세대에 동시 전멸하면 무승부 | K=20, R=1, 명세 예시 4의 셀 수 (세대 1~6: 5:4, 7: 6:5, 8: 3:3, 9: 1:2, 10: 0:0) | 세대 10에 종료, 무승부, 이유 = 전멸 | `#simultaneousWipeOutIsDraw` |
| TP-E-03 | 정상 | R라운드가 끝나면 셀이 많은 쪽이 이긴다 | R=1, K=4, 명세 예시 3의 셀 수 (매 세대 A=3·B=4) | 종료, B 승 (3:4), 이유 = 라운드 종료 | `#moreCellsWinsAfterLastRound` |
| TP-E-04 | 경계 | R라운드 종료 시 셀 수가 같으면 무승부 | R=1, K=3, 매 세대 A=4·B=4 (떨어진 block 두 개) | 종료, 무승부 (4:4), 이유 = 라운드 종료 | `#equalCountIsDrawAfterLastRound` |
| TP-E-05 | 경계 | 배치·개입 단계에서는 전멸을 검사하지 않는다 | 초기 배치에서 A만 1개 둔 상태(A=1·B=0) / 개입에서 A=0·B=4 | 게임 계속, 결과 없음 | `#noWipeOutCheckDuringPlacement` |
| TP-E-06 | 경계 | 셀 수가 바뀌는 진동자는 K의 홀짝에 따라 결과가 달라진다(건너뛰지 않음) | A beacon(세대 홀수 6, 짝수 8), B block(4), R=1, K=1과 K=2 | K=1: 6:4 A 승 / K=2: 8:4 A 승. 판정 셀 수가 K번째 세대의 값 | `#oscillatorCountUsesFullKGenerations` |
| TP-E-07 | 정상 | 점수는 현재 보드에 살아 있는 자기 셀 수다 | 명세 예시 4의 셀 수를 세대 7까지 진행 | `score(A)`=6, `score(B)`=5 | `#scoreIsCurrentLiveCellCount` |
| TP-E-08 | 예외 | 종료 뒤에는 선택·확정·패스·Tick 입력이 모두 무시된다 | TP-E-01로 종료한 상태에서 각 입력 | 모두 거부(Tick은 false), 세대 수·결과 변화 없음 | `#inputsIgnoredAfterGameOver` |
| TP-E-09 | 경계 | 마지막 라운드가 아니어도 K번째 세대에 전멸하면 개입으로 넘어가지 않고 종료한다 | R=2, K=3, 세대 3의 셀 수 A=2·B=0 | 라운드 1에서 종료, A 승, 이유 = 전멸 | `#wipeOutAtKthGenerationEndsGameBeforeIntervention` |
| TP-E-10 | 정상 | 판정 규칙만 숫자로 확인한다 (게임 없이) | 세대 직후 (0,3)·(3,0)·(0,0)·(2,3), 마지막 라운드 (5,3)·(3,5)·(4,4) | B 승·A 승·무승부·계속 / A 승·B 승·무승부 | `#judgeRulesOnNumbersOnly` |

## 5. UI 통합 (명세 4·5·8장, #7)

- 근거: [007](007-two-player-ui.md). 클래스: `TwoPlayerUiTest` (`test/com/holub/life/twoplayer/ui/`).
- CI가 headless(`java.awt.headless=true`)라 `JFrame`·`MenuSite`는 쓰지 않는다. `ModeSwitch`·`TwoPlayerPanel`(`JPanel`)·`BufferedImage` 렌더링과 대역 모드·대역 `TickSource`로 확인한다.
- 공통 전제: 2인용은 `TwoPlayerMode(settings, Runnable::run)`로 만든다(화면 갱신을 바로 실행). 패널의 보드 영역은 512×512(칸 8px). 클래식은 `ClassicMode`(`Neighborhood(8, Neighborhood(8, Resident))` + `GameController`)이고 (10,9)·(10,10)·(10,11)에 가로 blinker를 둔다. 시계는 `Clock` + 기록용 `TickSource` 대역이며 tick은 `clock.tick()`으로 직접 준다.
- 셀 1개는 1세대 뒤 죽어 전멸 판정으로 끝나므로, tick이 필요한 케이스(TP-U-09·10·11·16)는 블록(2×2, 정물)을 둔다.
- 상태 표시줄 문구는 영어 한 줄이다(기존 메뉴·대화상자와 같음). 테스트는 아래 조각이 들어 있는지만 본다: 단계(`Placement`, `Round r Simulation g/K`, `Round r Intervention`, `Game over`), 차례(`Turn: A`), 남은 개수(`Left A:x B:y`), 임시 배치(`Selected (r,c)`), 셀 수(`A:B = a:b`), 거부(`Rejected: <이유>`).

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| TP-U-01 | 정상 | 모드를 바꾸면 화면과 클릭 동작이 바뀐다 | Classic + 2 Players를 가진 `ModeSwitch`, 2 Players 선택 뒤 2인용 보드 (10,10) 칸 픽셀(84,84) 클릭 | 보이는 카드 = 2인용 패널. (10,10)이 A 임시 배치(토글 아님). 클래식 blinker 그대로 | `TwoPlayerUiTest#switchingModeChangesClickBehavior` |
| TP-U-02 | 정상 | 상태 표시줄에 단계·차례·남은 개수·A:B 셀 수가 나온다 | 초기 배치(N=20), A가 (10,10) 클릭 → 확정 | 문구에 `Placement`, `Turn: B`, `Left A:19 B:20`, `A:B = 1:0` | `#statusBarShowsGameState` |
| TP-U-03 | 정상 | 모델 상태가 바뀌면 상태 표시줄이 갱신된다 (Observer) | 패널을 만든 뒤 A가 (10,10) 클릭 | 세션 리스너가 1번 불리고, 상태 표시줄 라벨 문구가 세션 `status()`와 같아진다 | `#statusBarUpdatesOnModelChange` |
| TP-U-04 | 정상 | Enter 키로 임시 배치를 확정한다 | (10,10) 임시 배치 뒤 패널의 Enter 키 바인딩 실행 | TP-P-03과 같음: (10,10) A, 차례 B, A 남은 개수 N-1 | `#enterConfirmsPlacement` |
| TP-U-05 | 정상 | A·B 셀이 다른 색으로 그려지고 진영 경계선이 보인다 | A (10,10), B (10,40) 확정 뒤 512×512 `BufferedImage`에 렌더링 | (84,84) = A 색, (324,84) = B 색, 서로 다름. 빈 칸 (164,164) = 빈 칸 색. 31·32열 사이(x = 256)의 (256,300) = 경계선 색 | `#rendersOwnerColorsAndBorderLine` |
| TP-U-06 | 예외 | 2인용 모드에서는 Store(와 Clear·Load)를 쓸 수 없다 | Classic → 2 Players | 파일 명령 사용 가능 여부로 false를 받는다. 처음 Classic일 때는 true. `LifeFrame`은 이 값으로 Grid의 Clear·Load·Store 항목을 켜고 끈다(헤드리스라 메뉴 항목 자체는 확인하지 않음) | `#storeDisabledInTwoPlayerMode` |
| TP-U-07 | 예외 | 시뮬레이션 중 클릭은 무시되고 이유가 표시된다 | N=1, K=3: A (10,10), B (10,40) 확정 → 시뮬레이션. (20,20) 칸 클릭 | 보드 칸 변화 없음(임시 배치 없음, (20,20) 빈 칸), 문구에 `Rejected:` | `#clickDuringSimulationShowsReason` |
| TP-U-08 | 경계 | 2인용에서 클래식으로 돌아오면 기존 기능이 그대로 동작한다 | Classic → 2 Players → Classic | 파일 명령 사용 가능 = true. Tick: `clock.tick()` 1번에 blinker가 세로가 된다. 마우스 토글: `GameController.toggle(0,0)`으로 (0,0)이 산다. Clear: 모든 칸이 죽는다. Store → Load: 임시 파일에 저장한 blinker를 다시 불러오면 같은 칸. Go 속도: 같은 시계가 Fast로 돌고 멈춘다(메뉴 이름 → 시계 요청 매핑은 기존 `GameMenuTest#goCommandsKeepOriginalActions`). Exit은 `GameMenu.installMenus`가 그대로 단다(실제 종료는 확인하지 않음) | `#classicFeaturesWorkAfterSwitchingBack` |
| TP-U-09 | 정상 | 패스 버튼으로 개입 차례를 넘긴다 | N=4, K=1, M=1, R=2: A·B가 블록(2×2) 하나씩 배치 뒤 tick 1번 → 라운드 1 개입(선공 B). 패스 버튼 클릭 | 차례 A, 문구에 `Turn: A` | `#passButtonPassesTurn` |
| TP-U-10 | 정상 | 2인용 모드의 Tick은 시뮬레이션 세대를 진행하고 K에 들어간다 | N=4, K=3: A 블록 (10,10)~(11,11), B 블록 (10,40)~(11,41) 배치 → 시뮬레이션, 2 Players 선택, `clock.tick()` 1번 | `generationInRound` = 1, 문구에 `Simulation 1/3` | `#tickAdvancesTwoPlayerSimulation` |
| TP-U-11 | 경계 | 2인용 모드일 때 클래식 보드는 진행하지 않는다 | TP-U-10과 같은 상태에서 `clock.tick()` 3번 | 클래식 blinker 가로 그대로 | `#classicBoardDoesNotAdvanceInTwoPlayerMode` |
| TP-U-12 | 경계 | 배치 단계의 tick은 아무것도 바꾸지 않는다 | 초기 배치, 2 Players 선택, `clock.tick()` 1번 | 세션 리스너 0번, 문구 그대로, 단계 = 배치 | `#tickDuringPlacementIsIgnored` |
| TP-U-13 | 경계 | 모드 전환은 진행 중인 tick이 끝난 뒤에 된다 (#74 회피) | 대역 모드의 tick이 래치에서 멈춘 동안 다른 스레드에서 `select(다른 모드)` | 그 스레드는 BLOCKED, 지금 모드는 그대로. 래치를 풀면 전환이 끝나고 지금 모드 = 다른 모드 | `#switchWaitsForRunningTick` |
| TP-U-14 | 정상 | 모드를 바꾸면 시계가 멈춘다 | `clock.start(FAST)` 뒤 2 Players 선택 | `TickSource` 대역의 `stop()`이 불림, 실행 중 아님 | `#switchingModeStopsClock` |
| TP-U-15 | 정상 | 새 게임 버튼은 2인용 게임을 처음으로 되돌린다 | A (10,10) 확정, B (10,40) 임시 배치 뒤 새 게임 버튼 | 모든 칸 빈 칸, 임시 배치 없음, 단계 = 배치, 차례 A, `Left A:20 B:20` | `#newGameButtonResetsGame` |
| TP-U-16 | 경계 | 메뉴가 열려 있으면 2인용 tick도 건너뛴다 | TP-U-10의 시뮬레이션 상태, 메뉴 선택 경로가 있을 때 `clock.tick()` | `generationInRound` = 0. 선택 경로를 지운 뒤 `clock.tick()` → 1 | `#openMenuSuppressesTwoPlayerTick` |
| TP-U-17 | 예외 | 격자 밖 픽셀 클릭은 무시되고 이유가 표시된다 | 보드 영역 520×520(칸 8px, 격자 512px), 픽셀 (515,10) 클릭 | 임시 배치 없음, 문구에 `Rejected: outside the board` | `#clickOutsideGridShowsReason` |

- TP-U-08의 Exit은 `System.exit(0)`을 부르므로(`ui/GameMenu.java:23`) 실제 종료는 테스트하지 않는다.
- 이전 정의(2026-11-03)와 달라진 점: TP-U-01의 "셀 종류가 바뀐다"를 "화면(카드)이 바뀐다"로 고쳤다(007: 한 패널의 셀을 바꾸지 않고 모드마다 화면을 둔다). TP-U-06에 Clear·Load를 더했다. TP-U-09~17을 추가했다(패스·Tick·#74 회피·시계 정지·새 게임·메뉴 열림·격자 밖 클릭). TP-U-08의 `Universe.java` 근거를 지금 파일로 바꿨다.

## 기대값 검증 기록

- 방법: 명세 3장 규칙을 그대로 옮긴 Python 시뮬레이터(보드 밖 `DEAD`)를 따로 작성해 명세 7장 예시와 위 표의 격자 기대값을 다시 계산했다(2026-11-03, Son2kwon). 시뮬레이터는 저장소에 커밋하지 않았다. JUnit 구현이 들어오면 JUnit 결과가 기준이 된다.
- 결과
  - 예시 1~4: 명세의 보드·셀 수와 모두 일치.
  - 예시 5 대응 없음(세대 0~4 셀 수): 일치.
  - 예시 5 (5,8) 대응: 세대 30 보드와 B=24·A=0은 일치. 단, **명세의 "세대 10부터 B=24로 고정"은 재현되지 않았다.** 시뮬레이터에서는 B가 세대 10에 24가 된 뒤 40, 32, 40, 44, 56, 20을 거쳐 **세대 17부터 24로 고정**된다(A는 세대 8부터 0). TP-R-17은 시뮬레이터 결과를 기대값으로 썼다. 명세 오류, 세대 17이 맞음(별도 계산으로 교차 확인). [rules.md](rules.md)에는 고친 값을 반영했다.
  - TP-R-04, TP-R-05, TP-R-09, TP-E-06(beacon 8/6 교대)의 격자 기대값도 같은 시뮬레이터로 확인했다.
- 2026-11-15 (#5, Son2kwon): TP-R·TP-B·TP-I의 [CCB] 값은 `CellCandidateBenchmark`의 `CandidateF`(64×64)를 반사로 직접 구동하는 일회성 스크립트로 계산했다(`place` → `step` → 칸·셀 수 조회). 스크립트는 저장소에 커밋하지 않았다. 측정 클래스 자체(`main`)의 동등성 확인(F = G 칸별 소유자 200세대, F = 클래식 살아 있는 칸, 예시 4, TP-R-10)과 같은 F 코드다. 실제 2인용 셀이 들어오면 JUnit 결과가 기준이 된다.
  - 명세 예시 1~5를 같은 좌표로 64×64에서 돌린 결과가 명세 값과 모두 같았다(예시 5 (5,8) 대응: A=0은 세대 8부터, B는 세대 10에 24 → 40, 32, 40, 44, 56, 20 → 세대 17부터 24. 세대 30 칸 24개는 명세 그림과 같은 모양이고 16×16 안에 있다).
  - TP-R-11(블록 (1,0) 동쪽 가장자리 blinker), TP-B-07(정체 뒤 배치), TP-B-10·11(가득 찬 보드 2048:2048 → 세대 1 모서리 4칸 2:2 → 세대 2 0:0), TP-I-01~03(옮긴 배치의 세대별 셀 수)은 이 스크립트로만 계산한 값이다.
