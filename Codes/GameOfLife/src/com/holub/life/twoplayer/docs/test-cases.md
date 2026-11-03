# 2인용 모드 테스트 케이스

- 근거: 2인용 Life Game 규칙 명세(초안, 2026-11-02). 아래에서 "명세 N장"은 이 문서를 뜻한다.
- 상태: 정의 단계. 테스트 코드는 아직 없다. **테스트 클래스·메서드 이름은 예정**이고, 설계 결정(001~006)이 정해지면 실제 이름으로 바꾼다.
- 공통 전제
  - 좌표는 (행, 열)이고 0부터 센다. 보드 밖은 항상 `DEAD`다.
  - 격자 표기: `A`·`B`는 각 플레이어의 살아 있는 셀, `.`은 죽은 셀이다. 적지 않은 아래쪽 행은 모두 `.`이다.
  - 모든 테스트는 Swing 없이 모델만으로 돌린다(UI 케이스 제외). 세대 진행은 Clock 대신 수동 tick으로 하고, N·K·M·R은 작은 값을 주입한다(AGENTS.md 3-1).
  - 기대값 중 명세 7장 예시는 별도 Python 시뮬레이터로 다시 돌려 확인했다(아래 "기대값 검증 기록").

## 1. 세대 전이 규칙 (명세 3장, 이슈 2)

예정 클래스: `TwoPlayerRuleTest`

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| TP-R-01 | 정상 | 이웃 3개 중 A가 2개면 A로 태어난다 (명세 예시 1) | 5×5, A(1,1), A(2,2), B(3,1) | 1세대 뒤 (2,1)·(2,2)가 A, B=0 | `TwoPlayerRuleTest#bornAsMajorityColorA` |
| TP-R-02 | 정상 | 이웃 3개 중 B가 2개면 B로 태어난다 (TP-R-01의 색 반전) | 5×5, B(1,1), B(2,2), A(3,1) | 1세대 뒤 (2,1)·(2,2)가 B, A=0 | `#bornAsMajorityColorB` |
| TP-R-03 | 정상 | 상대 셀이 몰리면 내 셀이 과밀로 죽는다 (명세 예시 2) | 5×5, `.ABA.` / `..A..` / `..B..` (1~3행) | 1세대 뒤 (2,2) 사망, (0,2)에 A 탄생, A=3·B=1 | `#opponentNeighborsCauseOvercrowding` |
| TP-R-04 | 정상 | 살아 있는 셀은 이웃이 모두 상대 색이어도 자기 색을 유지한다 | 5×5, B(1,1), A(2,2), B(3,3) | 1세대 뒤 (2,2)가 A로 살아남음(이웃 2), B 둘은 고립사 | `#survivingCellKeepsItsColor` |
| TP-R-05 | 정상 | 같은 색 3개로 태어난 셀은 그 색이다 | 5×5, A(1,1)~(1,3) | 1세대 뒤 (0,2)·(1,2)·(2,2)가 A (세로 blinker) | `#bornFromThreeSameColor` |
| TP-R-06 | 정상 | 색을 가진 진동자도 주기마다 같은 색으로 돌아온다 | A blinker | 2세대마다 같은 보드, 모든 셀이 A | `#coloredBlinkerKeepsColorEachPeriod` |
| TP-R-07 | 경계 | 이웃이 1개 이하면 색과 상관없이 고립사 | TP-R-01의 B(3,1) (이웃 1) | 1세대 뒤 (3,1) `DEAD` | `#isolatedCellDies` |
| TP-R-08 | 경계 | 죽은 칸은 이웃이 4개면 태어나지 않는다 | 죽은 칸 주위에 A 2 + B 2 | 1세대 뒤 그 칸 `DEAD` | `#noBirthWithFourNeighbors` |
| TP-R-09 | 경계 | 보드 모서리에서는 바깥을 `DEAD`로 본다 | 4×4, A(0,1), A(1,0), A(1,1) | 1세대 뒤 (0,0)에 A 탄생, 2×2 block, A=4 | `#cornerTreatsOutsideAsDead` |
| TP-R-10 | 경계 | 하위 `Neighborhood` 경계(8칸 단위)를 넘는 칸에서도 탄생 색이 맞다 | 64×64, (7,7)·(7,8)·(8,7)에 A 2 + B 1 | 1세대 뒤 (8,8)이 A | `#birthAcrossNeighborhoodBoundary` |
| TP-R-11 | 경계 | 조용하던 `Neighborhood`도 이웃 블록의 탄생 때문에 다시 계산된다(활성 블록 최적화 회귀) | 64×64, 한 블록 가장자리 바로 옆에 탄생 조건을 만듦 | 탄생이 누락되지 않음 | `#quietNeighborhoodWakesOnEdgeBirth` |
| TP-R-12 | 정상 | 서로 닿지 않는 진동자와 정물은 영원히 같은 상태를 반복한다 (명세 예시 3) | 6×10, A `.AAA` (2,1)~(2,3), B block (2,6)~(3,7) | 매 세대 A=3·B=4, 세대 0과 2의 보드가 같음 | `#disjointPatternsRepeatForever` |
| TP-R-13 | 정상 | glider와 block이 부딪히면 같은 세대에 둘 다 전멸한다 (명세 예시 4) | 10×10, A glider (0,1)·(1,2)·(2,0)~(2,2), B block (5,5)~(6,6) | 세대 0~6 A=5·B=4, 7: 6:5, 8: 3:3, 9: 1:2, 10: 0:0 | `#gliderHitsBlockBothVanish` |
| TP-R-14 | 정상 | 섞인 이웃에서 다수색으로 태어나는 셀이 충돌 지점에 생긴다 (명세 예시 4) | TP-R-13과 같음 | 세대 7의 (5,4)가 B | `#mixedNeighborsBirthAtCollision` |
| TP-R-15 | 정상 | 색을 지우면 클래식 B3/S23과 살아 있는 칸이 항상 같다(불변조건) | 같은 초기 배치를 두 모드에 넣음: 예시 1~4, 시드 고정 무작위 보드 | 모든 세대에서 살아 있는 칸의 집합이 같음 | `#sameAliveCellsAsClassicWhenColorIgnored` |
| TP-R-16 | 정상 | 셀 1개 저격에 대응이 없으면 block이 무너진다 (명세 예시 5) | 16×16, B block (7,7)~(8,8), A(8,9) | 세대 0~4 A: 1,1,1,0,0 / B: 4,4,2,2,0 | `#snipeWithoutResponse` |
| TP-R-17 | 정상 | (5,8) 대응을 두면 B가 살아남는다 (명세 예시 5) | TP-R-16 + B(5,8) | 세대 8부터 A=0, 세대 17부터 B=24로 고정, 세대 30 보드가 명세 그림과 같음 | `#snipeWithResponseAt5_8` |
| TP-R-18 | 예외 | 2인용 셀을 추가해도 클래식 모드 동작은 바뀌지 않는다 | 이슈 1의 특성 테스트 전체 | 수정 없이 모두 통과 | (이슈 1의 특성 테스트 클래스) |

TP-R-16·17은 전이 규칙만 확인한다(승패 판정 없이 30세대 진행). 같은 보드를 게임 진행에 넣으면 A가 먼저 0개가 되어 그 세대에 종료된다(6장). 명세 예시 5는 보드의 다른 셀을 생략한 국소 예시이기 때문이다.

## 2. 게임 진행 (명세 4장, 이슈 3)

예정 클래스: `TwoPlayerGameFlowTest`. 별도 표시가 없으면 N=2, K=3, M=2, R=2를 주입한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| TP-F-01 | 정상 | 새 게임은 초기 배치 단계에서 A 차례로 시작한다 | 새 게임 | 단계 = 초기 배치, 차례 = A, 남은 개수 A=N·B=N | `TwoPlayerGameFlowTest#startsInPlacementWithA` |
| TP-F-02 | 정상 | 초기 배치는 1개씩 번갈아 둔다 | A 확정 → B 확정 → A 확정 | 차례가 A → B → A → B 순서로 바뀜 | `#placementAlternatesOneByOne` |
| TP-F-03 | 정상 | 양쪽이 N개씩 두면 시뮬레이션 단계로 넘어간다 | A·B 각 2개 확정 | 단계 = 시뮬레이션, 세대 수 0 | `#placementEndsAfterNEach` |
| TP-F-04 | 정상 | 시뮬레이션은 K세대 뒤 개입 단계로 넘어간다 | 시뮬레이션 단계, 수동 tick 3회 | 2회까지는 시뮬레이션, 3회 뒤 단계 = 개입 | `#simulationEndsAfterKGenerations` |
| TP-F-05 | 정상 | 개입 단계를 시작하면 각자 M개를 받는다 | 개입 단계 진입 | 남은 개수 A=M·B=M | `#interventionGrantsMEach` |
| TP-F-06 | 정상 | 두 사람이 연달아 패스하면 개입이 끝난다 | 개입: 선공 패스 → 후공 패스 | 다음 라운드 시뮬레이션 단계로 넘어감 | `#twoConsecutivePassesEndIntervention` |
| TP-F-07 | 경계 | 패스 사이에 배치가 끼면 "연달아"가 아니다 | 개입: 패스 → 배치 → 패스 | 아직 개입 단계, 차례가 넘어감 | `#passPlacePassDoesNotEnd` |
| TP-F-08 | 정상 | 개입 선공은 직전 라운드의 후공이 맡는다 | R=3, 라운드 1·2의 개입 시작 | 라운드 1 개입 선공 = B(초기 배치 후공), 라운드 2 개입 선공 = A | `#interventionFirstPlayerAlternates` |
| TP-F-09 | 경계 | 개수가 0인 플레이어는 자동으로 패스한다 | 개입: A가 M개를 모두 씀 | A 차례가 오면 자동 패스 처리, B가 패스하면 개입 종료 | `#autoPassWhenNoPlacementsLeft` |
| TP-F-10 | 경계 | 둘 빈칸이 없는 플레이어는 자동으로 패스한다 | 빈칸이 없는 작은 보드에서 개입 진입 | 양쪽 자동 패스, 개입 즉시 종료 | `#autoPassWhenNoEmptyCell` |
| TP-F-11 | 경계 | 쓰지 않은 개수는 다음 라운드로 넘어가지 않는다 | 라운드 1 개입에서 1개만 쓰고 패스 | 라운드 2 개입 시작 시 남은 개수 = M (M+1 아님) | `#unusedPlacementsAreDiscarded` |
| TP-F-12 | 정상 | R번째 라운드의 시뮬레이션 뒤에는 개입 없이 종료한다 | R=2, 라운드 2 시뮬레이션 K세대 완료 | 단계 = 종료 | `#gameEndsAfterLastSimulation` |
| TP-F-13 | 경계 | 보드가 정체돼도 K세대를 모두 진행한다 | 정물(block)만 있는 보드, K=3 | 세대 수가 3이 된 뒤에야 개입 단계 | `#stableBoardStillRunsAllKGenerations` |
| TP-F-14 | 정상 | 시뮬레이션 단계의 수동 Tick은 1세대로 K에 포함된다 | 시뮬레이션, 수동 Tick 1회 + 자동 진행 | 합계 K세대에서 개입으로 넘어감 | `#manualTickCountsTowardK` |
| TP-F-15 | 예외 | 시뮬레이션이 아닌 단계의 Tick은 무시된다 | 초기 배치·개입 단계에서 Tick | 보드·세대 수 변화 없음 | `#tickIgnoredOutsideSimulation` |
| TP-F-16 | 정상 | 설정을 주입하지 않으면 기본값을 쓴다 | 기본 생성 | N=20, K=20, M=3, R=5 | `#defaultSettings` |
| TP-F-17 | 예외 | 잘못된 설정값은 거부한다 | N·K·R ≤ 0 또는 M < 0 | `IllegalArgumentException` | `#rejectsInvalidSettings` |

TP-F-08은 명세 4장 "선공은 직전 라운드의 후공"을 라운드 1에서는 "초기 배치의 후공(B)"으로 해석한 것이다. **해석 확인 필요.** TP-F-17은 명세에 없는 방어 규칙이다(설정값으로 뺀다는 4장 결정에서 나옴).

## 3. 배치 규칙 (명세 5장, 이슈 3)

예정 클래스: `PlacementRuleTest`. 보드는 64×64이고 A 진영은 0~31열, B 진영은 32~63열이다.

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
| TP-P-10 | 예외 | 남은 개수가 0이면 둘 수 없다 | 남은 개수 0인 플레이어의 배치 요청 | 거부, 이유 = 남은 개수 없음 | `#rejectsWhenNoPlacementsLeft` |
| TP-P-11 | 예외 | 시뮬레이션·종료 단계에서는 배치 입력을 받지 않는다 | 시뮬레이션·종료 단계에서 클릭 | 거부, 이유 = 배치 단계 아님, 보드 변화 없음 | `#rejectsPlacementOutsidePlacementPhases` |
| TP-P-12 | 예외 | 보드 밖 좌표는 거부한다 | (-1,0), (0,64), (64,0) | 거부, 예외로 죽지 않음 | `#rejectsOutOfBoardCoordinate` |
| TP-P-13 | 예외 | 임시 배치 없이 확정할 수 없다 | 아무것도 클릭하지 않고 확정 | 거부, 차례 그대로 | `#confirmWithoutTentativeIsRejected` |
| TP-P-14 | 예외 | 초기 배치에서는 패스할 수 없다 | 초기 배치에서 패스 | 거부, 차례 그대로 | `#passRejectedDuringPlacement` |
| TP-P-15 | 경계 | 초기 배치의 마지막 수로 경계 건너편 상대 셀 옆에 두는 것은 허용된다(의도된 위험) | A가 (10,31)에 둔 뒤, B의 N번째 수로 (10,32) | 허용 | `#lastPlacementNextToBorderIsAllowed` |

TP-P-13은 명세에 직접 적혀 있지 않다. 초기 배치에 패스가 없고 "확정하면 차례가 넘어간다"는 규칙에서 나온 해석이다. **해석 확인 필요.**

## 4. 종료 조건과 승패 판정 (명세 6장, 이슈 3)

예정 클래스: `GameJudgeTest`

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| TP-E-01 | 정상 | 한쪽이 전멸하면 K세대를 기다리지 않고 바로 끝난다 | K=10, A는 고립 셀 1개, B는 block | 세대 1에 종료, B 승, 세대 수 1 | `GameJudgeTest#oneSideWipedOutEndsImmediately` |
| TP-E-02 | 정상 | 같은 세대에 동시 전멸하면 무승부 | K=20, 명세 예시 4 보드 | 세대 10에 종료, 무승부 | `#simultaneousWipeOutIsDraw` |
| TP-E-03 | 정상 | R라운드가 끝나면 셀이 많은 쪽이 이긴다 | R=1, K=4, 명세 예시 3 보드 | 종료, B 승 (4:3) | `#moreCellsWinsAfterLastRound` |
| TP-E-04 | 경계 | R라운드 종료 시 셀 수가 같으면 무승부 | A block과 B block이 떨어져 있음, R=1 | 종료, 무승부 (4:4) | `#equalCountIsDrawAfterLastRound` |
| TP-E-05 | 경계 | 배치·개입 단계에서는 전멸을 검사하지 않는다 | 초기 배치, A만 1개 둔 상태(B=0) | 게임 계속, 단계 = 초기 배치 | `#noWipeOutCheckDuringPlacement` |
| TP-E-06 | 경계 | 셀 수가 바뀌는 진동자는 K의 홀짝에 따라 결과가 달라진다(건너뛰지 않음) | A beacon(8/6), B block(4), R=1, K=1과 K=2 | K=1: 6:4 A 승 / K=2: 8:4 A 승, 판정 시점 셀 수가 K세대 결과와 같음 | `#oscillatorCountUsesFullKGenerations` |
| TP-E-07 | 정상 | 점수는 현재 보드에 살아 있는 자기 셀 수다 | 명세 예시 4 세대 7 | A=6, B=5 | `#scoreIsCurrentLiveCellCount` |
| TP-E-08 | 예외 | 종료 뒤에는 배치·패스·Tick 입력이 모두 무시된다 | 종료 상태에서 각 입력 | 보드·결과 변화 없음 | `#inputsIgnoredAfterGameOver` |

## 5. UI 통합 (명세 4·5·8장, 이슈 4)

예정 클래스: `TwoPlayerUiTest`. Swing 창을 띄우지 않고 대역(Stub)이나 `BufferedImage` headless 렌더링으로 확인한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| TP-U-01 | 정상 | 모드를 바꾸면 셀 종류와 클릭 동작이 바뀐다 | Classic → 2 Players | 클릭이 토글이 아니라 현재 플레이어 임시 배치로 동작 | `TwoPlayerUiTest#switchingModeChangesClickBehavior` |
| TP-U-02 | 정상 | 상태 표시줄에 단계·차례·남은 개수·A:B 셀 수가 나온다 | 초기 배치, A 1개 확정 뒤 | 문구에 단계, 차례 B, 남은 개수, A:B = 1:0 포함 | `#statusBarShowsGameState` |
| TP-U-03 | 정상 | 모델 상태가 바뀌면 상태 표시줄이 갱신된다 | 배치 확정 이벤트 | 상태 표시줄 대역이 새 문구를 받음 | `#statusBarUpdatesOnModelChange` |
| TP-U-04 | 정상 | Enter 키로 임시 배치를 확정한다 | 임시 배치 뒤 Enter | TP-P-03과 같은 결과 | `#enterConfirmsPlacement` |
| TP-U-05 | 정상 | A·B 셀이 다른 색으로 그려지고 진영 경계선이 보인다 | A·B 셀이 있는 보드를 `BufferedImage`에 렌더링 | A 칸·B 칸 픽셀 색이 서로 다름, 31·32열 사이에 경계선 색 픽셀 | `#rendersOwnerColorsAndBorderLine` |
| TP-U-06 | 예외 | 2인용 모드에서는 Store를 쓸 수 없다 | 2 Players 모드 | Store 메뉴 비활성화 | `#storeDisabledInTwoPlayerMode` |
| TP-U-07 | 예외 | 시뮬레이션 중 클릭은 무시되고 이유가 표시된다 | 시뮬레이션 단계에서 클릭 | 보드 변화 없음, 상태 표시줄에 거부 이유 | `#clickDuringSimulationShowsReason` |
| TP-U-08 | 경계 | 2인용에서 클래식으로 돌아오면 기존 기능이 그대로 동작한다 | 2 Players → Classic | Clear, Load, Store, Go 속도/Tick, 마우스 토글이 클래식 특성 테스트와 같은 결과. Exit은 메뉴 항목 존재만 확인 | `#classicFeaturesWorkAfterSwitchingBack` |

TP-U-08의 Exit은 `System.exit(0)`을 호출하므로(`Universe.java:116-124`) 실제 종료는 테스트하지 않는다.

## 기대값 검증 기록

- 방법: 명세 3장 규칙을 그대로 옮긴 Python 시뮬레이터(보드 밖 `DEAD`)를 따로 작성해 명세 7장 예시와 위 표의 격자 기대값을 다시 계산했다(2026-11-03, Son2kwon). 시뮬레이터는 저장소에 커밋하지 않았다. JUnit 구현이 들어오면 JUnit 결과가 기준이 된다.
- 결과
  - 예시 1~4: 명세의 보드·셀 수와 모두 일치.
  - 예시 5 대응 없음(세대 0~4 셀 수): 일치.
  - 예시 5 (5,8) 대응: 세대 30 보드와 B=24·A=0은 일치. 단, **명세의 "세대 10부터 B=24로 고정"은 재현되지 않았다.** 시뮬레이터에서는 B가 세대 10에 24가 된 뒤 40, 32, 40, 44, 56, 20을 거쳐 **세대 17부터 24로 고정**된다(A는 세대 8부터 0). TP-R-17은 시뮬레이터 결과를 기대값으로 썼다. 명세 수정 여부는 명세 작성자와 확인한다.
  - TP-R-04, TP-R-05, TP-R-09, TP-E-06(beacon 8/6 교대)의 격자 기대값도 같은 시뮬레이터로 확인했다.
