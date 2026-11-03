# 클래식 모드 특성 테스트 케이스

- 관련 이슈: #4
- 목적: 리팩토링·기능 추가 전에 **현재 동작을 그대로 고정**한다(AGENTS.md 강령 1). 이후 모든 PR에서 이 테스트가 수정 없이 통과해야 한다.
- 원칙
  - 기대값은 "옳은 동작"이 아니라 **현재 코드가 실제로 하는 동작**이다. 버그로 보이는 동작도 그대로 고정하고, 고치지 않고 별도 이슈로 남긴다(강령 2). 이런 케이스는 분류 옆에 `현재 동작`이라고 표시했다.
  - 프로덕션 코드는 바꾸지 않는다. 테스트 코드만 추가한다.
  - Swing 없이 돈다. 보드는 `Universe`와 같은 구성(`new Neighborhood(8, new Neighborhood(8, new Resident()))`, 64×64, `Universe.java:42-43`)으로 테스트에서 직접 만든다.

## 테스트 도우미

| 도우미 | 하는 일 | 원본 코드에서 같은 동작을 하는 곳 |
| --- | --- | --- |
| `ClassicBoard#click(row, col)` | 픽셀 1개 = 셀 1개인 64×64 영역으로 `userClicked` 호출 | 마우스 클릭 (`Universe.java:79`) |
| `ClassicBoard#tick()` | 바깥을 `Cell.DUMMY` 8개로 두고 `figureNextState` → true면 `transition` | Clock 리스너 (`Universe.java:127-141`) |
| `ClassicBoard#clear()` | `clear()` 호출 | Grid > Clear (`Universe.java:90`) |
| `ClassicBoard#store()` | `createMemento()` → `transfer(STORE)` → `flush` | Grid > Store (`Universe.java:180-182`) |
| `ClassicBoard#load(bytes)` | `clear()` → `createMemento()` → `load` → `transfer(LOAD)` | Grid > Load (`Universe.java:159-163`) |
| `ClassicBoard#liveCells()` | 메모리 안에서 `createMemento().flush()`한 결과를 읽어 살아 있는 칸 집합을 돌려줌 | — (셀 상태를 읽는 공개 API가 없어서 Memento를 씀) |
| `ReferenceLife` | 2차원 boolean 배열로 만든 단순 B3/S23 (보드 밖 = 죽은 셀) | — (비교 기준) |

## Swing 없이 테스트할 수 없는 것

- `Universe`, `Clock`은 생성자에서 `MenuSite.addLine()`을 호출하고, `MenuSite`는 `JFrame`으로 `establish()`돼 있어야 한다(`MenuSite.java:177-178`의 assert). 그래서 headless 환경에서는 두 객체를 만들 수 없다.
- 따라서 Go 메뉴(속도·Tick), Exit, 파일 선택 대화상자는 이번에 직접 테스트하지 않는다. 대신 각 메뉴가 호출하는 모델 쪽 경로를 위 도우미로 똑같이 재현해 고정한다. 메뉴 자체의 테스트는 Clock·UI 리팩토링으로 의존성을 끊은 뒤 추가한다.

## 1. 세대 진행 (Tick)

예정 클래스: `ClassicGenerationTest`

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| CL-G-01 | 정상 | 정물(block)은 tick을 해도 변하지 않는다 | (10,10)·(10,11)·(11,10)·(11,11) | 5 tick 뒤에도 같은 4칸 | `ClassicGenerationTest#blockIsStill` |
| CL-G-02 | 정상 | blinker는 2세대마다 원래 모양으로 돌아온다 | (10,9)·(10,10)·(10,11) | 1 tick 뒤 (9,10)·(10,10)·(11,10), 2 tick 뒤 처음 모양 | `#blinkerReturnsEveryTwoGenerations` |
| CL-G-03 | 정상 | glider는 4세대 뒤 오른쪽 아래로 1칸 이동한다 | (1,2)·(2,3)·(3,1)·(3,2)·(3,3) | 4 tick 뒤 모든 칸이 (+1,+1) 이동 | `#gliderMovesDiagonallyEveryFourGenerations` |
| CL-G-04 | 경계 | 빈 보드는 tick을 해도 빈 보드다 | 빈 보드 | 3 tick 뒤 살아 있는 칸 0 | `#emptyBoardStaysEmpty` |
| CL-G-05 | 경계 | 보드 모서리에서는 바깥을 죽은 셀로 본다 | (0,1)·(1,0)·(1,1) | 1 tick 뒤 (0,0)에 탄생해 block 4칸 | `#cornerTreatsOutsideAsDead` |
| CL-G-06 | 경계 | glider가 하위 `Neighborhood`(8칸) 경계를 넘어도 모양이 유지된다 | CL-G-03 glider, 32 tick (8칸 이동 → 경계 1번 넘음) | 처음 모양에서 (+8,+8) 이동 | `#gliderKeepsShapeAcrossNeighborhoodBoundary` |
| CL-G-07 | 경계 | 하위 `Neighborhood`의 가장자리 한 행 전체가 동시에 바뀌어도 결과가 단순 구현과 같다 | (7,0)~(7,7) 8칸 | 30 tick 동안 매 세대 `ReferenceLife`와 같음 | `#fullNeighborhoodEdgeRowMatchesReference` |
| CL-G-08 | 정상 | 64×64 보드 전체의 결과가 단순 구현과 같다 (활성 블록 최적화가 결과를 바꾸지 않음) | 시드 42 `Random`, 칸마다 1/4 확률로 생존, 5개 보드 | 각 100 tick 동안 매 세대 `ReferenceLife`와 같음 | `#randomBoardsMatchReference` |
| CL-G-09 | 정상 | 오래 변하는 패턴도 단순 구현과 같다 | R-pentomino (30,31)·(30,32)·(31,30)·(31,31)·(32,31) | 300 tick 동안 매 세대 `ReferenceLife`와 같음 | `#rPentominoMatchesReference` |
| CL-G-10 | 경계 | glider가 보드 가장자리에 닿은 뒤에도 결과가 단순 구현과 같다 | CL-G-03 glider, 240 tick | 매 세대 `ReferenceLife`와 같음 | `#gliderHittingBoardEdgeMatchesReference` |

## 2. 마우스 토글과 Clear

예정 클래스: `ClassicEditTest`

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| CL-E-01 | 정상 | 죽은 칸을 클릭하면 살아난다 | 빈 보드, (5,7) 클릭 | 살아 있는 칸 = {(5,7)} | `ClassicEditTest#clickRevivesDeadCell` |
| CL-E-02 | 정상 | 산 칸을 다시 클릭하면 죽는다 | (5,7) 두 번 클릭 | 빈 보드 | `#clickTwiceKillsCell` |
| CL-E-03 | 경계 | 보드 네 모서리 칸도 클릭할 수 있다 | (0,0)·(0,63)·(63,0)·(63,63) 클릭 | 네 칸 모두 살아 있음 | `#cornerCellsAreClickable` |
| CL-E-04 | 정상 | 클릭으로 만든 패턴은 다음 tick부터 규칙대로 진행한다 | blinker를 클릭으로 만들고 1 tick | CL-G-02의 1 tick 결과와 같음 | `#clickedPatternEvolvesOnTick` |
| CL-E-05 | 정상 | Clear는 모든 칸을 죽인다 | blinker + block | 빈 보드 | `#clearKillsAllCells` |
| CL-E-06 | 경계 | Clear 뒤 tick을 해도 아무것도 살아나지 않는다 | blinker → Clear → 1 tick | 빈 보드 | `#nothingRevivesAfterClear` |
| CL-E-07 | 예외 (`현재 동작`) | 보드 밖 좌표로 클릭하면 예외가 난다 | (64,0), (-1,0) | `ArrayIndexOutOfBoundsException` | `#clickOutsideBoardThrows` |

CL-E-07: 실제 화면에서는 `Universe`가 패널 크기를 보드 크기에 맞추므로(`Universe.java:49-64`) 이 좌표가 들어오기 어렵다. 버그로 보지 않고 현재 동작으로만 기록한다.

## 3. 저장·불러오기 (Store / Load, Memento)

예정 클래스: `ClassicMementoTest`

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| CL-M-01 | 정상 | 저장한 보드를 불러오면 같은 칸이 살아 있다 | glider + block 저장 → 새 보드에 불러오기 | 살아 있는 칸 집합이 같음 | `ClassicMementoTest#storeThenLoadRestoresSameCells` |
| CL-M-02 | 정상 | 불러오기는 기존 보드를 지우고 덮어쓴다 | 보드 X에 blinker가 있는 상태에서 block 파일 불러오기 | block만 살아 있음 | `#loadReplacesExistingCells` |
| CL-M-03 | 정상 | 불러온 보드도 tick하면 규칙대로 진행한다 | blinker 저장 → 불러오기 → 1 tick | CL-G-02의 1 tick 결과와 같음 | `#loadedBoardEvolvesOnTick` |
| CL-M-04 | 정상 | 저장 형식은 `java.awt.Point`(x=열, y=행)의 직렬화된 `Collection`이다 | (5,6) 하나 저장 | 파일을 읽으면 `Collection`이고 원소가 `Point(6,5)` | `#storedFormatIsSerializedPointCollection` |
| CL-M-05 | 경계 | 빈 보드를 저장·불러오기 할 수 있다 | 빈 보드 저장 → 불러오기 | 빈 보드, 예외 없음 | `#emptyBoardRoundTrip` |
| CL-M-06 | 경계 (`현재 동작`) | Store 경로는 살아 있는 칸을 두 번씩 기록한다 | 2칸 저장 (`Universe.doStore`와 같은 순서) | 저장된 `Collection` 크기 4, 각 칸이 2번씩 | `#storeRecordsEachLiveCellTwice` |
| CL-M-07 | 예외 | 직렬화 형식이 아닌 파일을 불러오면 `IOException` | `"garbage"` 바이트 | `IOException`(실제 `StreamCorruptedException`) | `#loadingGarbageThrowsIOException` |
| CL-M-08 | 예외 (`현재 동작`) | `Collection`이 아닌 객체가 든 파일을 불러오면 `IOException`이 아니라 `ClassCastException` | 직렬화된 `String` | `ClassCastException` | `#loadingNonCollectionThrowsClassCastException` |
| CL-M-09 | 예외 | 단일 셀과 `DUMMY`는 Memento를 만들 수 없다 | `new Resident().createMemento()`, `Cell.DUMMY.createMemento()` | 둘 다 `UnsupportedOperationException` | `#unitaryCellsCannotCreateMemento` |

- CL-M-06: `Universe.doStore()`는 `createMemento()`(내부에서 이미 `transfer(STORE)`, `Neighborhood.java:400`) 뒤에 `transfer(STORE)`를 한 번 더 부른다(`Universe.java:180-181`). 불러올 때는 `contains()`로만 확인해서 화면 결과는 같다. → 버그 후보로 이슈 등록 예정.
- CL-M-08: `NeighborhoodState.load()`는 읽은 객체를 확인 없이 `Collection`으로 캐스팅한다(`Neighborhood.java:421`). `Universe.doLoad()`는 `IOException`만 잡으므로(`Universe.java:166`) 이 예외는 "Read Failed!" 대화상자 없이 이벤트 스레드까지 올라간다. → 버그 후보로 이슈 등록 예정.

## 4. 셀 단위 동작

예정 클래스: `ClassicCellTest`

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| CL-C-01 | 정상 | 새 `Resident`는 죽어 있다 | `new Resident()` | `isAlive() == false` | `ClassicCellTest#newResidentIsDead` |
| CL-C-02 | 정상 | 고립된 산 셀은 다음 상태가 바뀐다고 알리고, transition 뒤 죽는다 | 산 `Resident`, 이웃 모두 `DUMMY` | `figureNextState` = true, transition 뒤 `isAlive()` = false | `#isolatedLiveResidentDies` |
| CL-C-03 | 경계 (`현재 동작`) | `Resident.transition()`의 반환값은 "바뀌었는가"의 반대다 | CL-C-02의 셀 / 계속 죽어 있는 셀 | 바뀐 셀은 false, 안 바뀐 셀은 true | `#residentTransitionReturnsInvertedFlag` |
| CL-C-04 | 정상 | `DUMMY`는 항상 죽어 있고 가장자리 요청에 자기 자신을 돌려준다 | `Cell.DUMMY` | `isAlive()` = false, `edge(0,0)`과 `edge(5,5)`가 `DUMMY` | `#dummyIsAlwaysDead` |
| CL-C-05 | 경계 (`현재 동작`) | `Neighborhood.isAlive()`는 빈 보드에서도 true다 | 빈 64×64 보드 | `isAlive()` = true | `#neighborhoodIsAliveIsAlwaysTrue` |
| CL-C-06 | 정상 | 보드 폭은 64칸이다 | `Universe`와 같은 구성 | `widthInCells()` = 64 | `#boardIs64CellsWide` |
| CL-C-07 | 예외 | `Neighborhood.edge()`에 가운데 칸을 요청하면 assert가 실패한다 | 8×8 `Neighborhood`의 `edge(3,3)` | `AssertionError` (테스트는 `-ea`로 실행) | `#edgeRejectsCentralCell` |

- CL-C-03: AGENTS.md 강령 2에 예로 나온 동작이다(`Resident.java:79-83`). 세대 진행 결과는 CL-G-07~10에서 단순 구현과 같다는 것을 확인했으므로 화면 결과에는 영향이 없다. 이미 알려진 항목이라 별도 이슈 등록 여부는 팀과 정한다.
- CL-C-05: 이 메서드를 호출하는 곳은 저장소에 없다. Cell 인터페이스 회의 안건(`docs/001-cell-state-representation.md`, `feature/two-player-mode` 브랜치)에 포함돼 있다.

## 사전 확인 기록

테스트 코드를 쓰기 전에, 원본 클래스(`./gradlew compileJava` 결과)를 불러 쓰는 탐색용 프로그램으로 위 기대값을 먼저 확인했다(2026-11-03, JDK 26, `-ea`, headless). CL-G-07~10의 비교 대상은 같은 프로그램 안의 단순 구현이고, 모든 세대에서 일치했다.
