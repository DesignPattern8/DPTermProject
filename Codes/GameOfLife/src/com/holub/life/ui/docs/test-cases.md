# D 도메인 테스트 케이스 정의

- 테스트 위치(예정): `Codes/GameOfLife/test/com/holub/life/ui/`
- 실행 조건: `-Djava.awt.headless=true`. 창을 띄우지 않고 `BufferedImage`와 가짜 대상 없이 실행 가능한 범위만 자동화한다.
- 상태: **정의 완료, 테스트 코드는 2단계(A·B 구현과 JUnit 설정이 들어온 뒤)에 작성**
- `특성` 표시는 기존 동작을 고정하는 테스트다. 기존 `Universe`/`Neighborhood` 코드를 지우기 전에 먼저 실행해 기준을 만든다.

## 좌표 변환 (`GridCoordinateMapper`)

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| UI-01 | 정상(특성) | 새 좌표 변환이 기존 `Neighborhood.userClicked`와 같은 셀을 고르는지 확인 | 64×64 보드, 512px, 보드 전체를 일정 간격으로 클릭 | 기존 방식으로 토글된 셀과 `toCell` 결과가 모든 지점에서 같다 | `GridCoordinateMapperTest#matchesLegacyClickMapping` |
| UI-02 | 경계 | 블록 경계·보드 끝 픽셀에서 올바른 셀을 고르는지 확인 | (0,0), (63,63), (64,64), (511,511) | (0,0), (7,7), (8,8), (63,63) | `GridCoordinateMapperTest#mapsBlockAndBoardEdges` |
| UI-03 | 경계 | 보드 밖 클릭은 요청하지 않는지 확인 | (-1,0), (0,-1), (512,0), (0,512) | 빈 결과 | `GridCoordinateMapperTest#outsideBoardIsEmpty` |
| UI-04 | 경계 | 크기 조정 시 모든 셀이 같은 크기가 되도록 맞추는지 확인 (`Universe.java:50~61`) | 높이 500, 512, 600 / 셀 64개 | 448, 512, 576 | `GridCoordinateMapperTest#snapsToMultipleOfCells` |
| UI-05 | 예외 | 잘못된 보드 크기를 거부하는지 확인 | rows=60, blockSize=8 / rows=0 | `IllegalArgumentException` | `GridCoordinateMapperTest#rejectsInvalidDimensions` |

## 렌더링 (`BoardRenderer` 계열)

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| UI-06 | 정상(특성) | 렌더링 분리 후 화면이 기존과 같은지 확인 | 글라이더(블록 경계를 넘는 위치) + 블링커, 0~6세대, 512px | 기존 `redraw(g, rect, true)` 이미지와 픽셀 일치 | `BoardRendererTest#sameOutputAsLegacy` |
| UI-07 | 경계 | 셀 크기가 나누어떨어지지 않는 크기에서도 기존과 같은지 확인 | UI-06과 같은 보드, 640px | 기존 이미지와 픽셀 일치 | `BoardRendererTest#sameOutputAsLegacyAtOtherSize` |
| UI-08 | 정상 | 빈 보드는 모든 셀이 죽은 색인지 확인 | 빈 스냅샷 | 셀 내부 픽셀이 모두 `LIGHT_YELLOW`, 파란 테두리 없음 | `BoardRendererTest#emptyBoardHasNoLiveOrActiveColor` |
| UI-09 | 정상 | 활성 블록에만 파란 테두리가 그려지는지 확인 | 블록 (1,2)만 활성 | 해당 블록 안쪽 테두리만 `BLUE` | `BoardRendererTest#onlyActiveBlockIsOutlined` |
| UI-10 | 정상 | Decorator를 빼면 해당 표시만 빠지는지 확인 | `CellLayerRenderer` 단독 | 주황·파란 테두리가 없고 셀은 같다 | `BoardRendererTest#decoratorsAddOnlyTheirOverlay` |

## 화면 갱신·입력 (`BoardPanel`)

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| UI-11 | 정상 | 클릭이 좌표로 바뀌어 컨트롤러에 전달되는지 확인 | 512px 패널, (100, 200) 클릭 | `toggleCell(CellPosition(25, 12))` 한 번 호출 | `BoardPanelTest#clickRequestsToggle` |
| UI-12 | 경계 | 보드 밖 클릭은 컨트롤러를 부르지 않는지 확인 | 패널이 보드보다 큰 상태에서 보드 밖 클릭 | `toggleCell` 호출 없음 | `BoardPanelTest#clickOutsideBoardDoesNothing` |
| UI-13 | 정상 | 알림을 받으면 새 스냅샷으로 그리는지 확인 | `boardChanged(s2)` 호출 후 `paintComponent` | `s2` 기준 이미지 | `BoardPanelTest#paintsLatestSnapshot` |
| UI-14 | 정상 | 생성 시 컨트롤러에 리스너로 등록되는지 확인 | 패널 생성 | `addListener(panel)` 한 번 호출 | `BoardPanelTest#registersAsListener` |

## 메뉴 (`GameMenu`)

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| UI-15 | 정상(특성) | 메뉴 구성이 기존과 같은지 확인 | `entries()` | Grid: Clear, Load, Store, Exit / Go: Halt, Tick (Single Step), Agonizing, Slow, Medium, Fast (순서 포함) | `GameMenuTest#sameMenusAsLegacy` |
| UI-16 | 정상(특성) | 속도 메뉴가 기존 간격으로 시작하는지 확인 | Agonizing, Slow, Medium, Fast 실행 | `start(500)`, `start(150)`, `start(70)`, `start(30)` | `GameMenuTest#speedsMatchLegacy` |
| UI-17 | 정상 | Halt, Tick, Clear가 맞는 요청으로 이어지는지 확인 | 각 항목 실행 | `stop()`, `step()`, `clear()` | `GameMenuTest#controlEntriesForward` |
| UI-18 | 정상 | Exit가 종료 동작을 부르는지 확인 (실제 종료 대신 주입) | Exit 실행 | 주입한 종료 동작 한 번 호출 | `GameMenuTest#exitRunsExitAction` |
| UI-19 | 정상 | Load가 고른 파일로 컨트롤러를 부르는지 확인 | 파일 선택 결과 `a.life` | `load(a.life)`, 오류 표시 없음 | `GameMenuTest#loadForwardsChosenFile` |
| UI-20 | 예외 | 불러오기 실패 시 오류를 표시하는지 확인 | `load`가 `IOException` | `"Read Failed!"` 표시 | `GameMenuTest#loadFailureShowsError` |
| UI-21 | 예외 | 저장 실패 시 오류를 표시하는지 확인 | `store`가 `IOException` | `"Write Failed!"` 표시 | `GameMenuTest#storeFailureShowsError` |
| UI-22 | 예외(특성) | 파일 선택 취소 시 기존처럼 동작하는지 확인 | 선택 결과 없음 | `load`/`store` 호출 없음, 기존처럼 실패 메시지 표시 (아래 참고) | `GameMenuTest#cancelBehavesLikeLegacy` |

- UI-18~22는 `FileDialogs`를 상속한 테스트용 클래스로 파일 선택 결과와 오류 표시를 대신한다. 실제 대화상자는 띄우지 않는다.
- **UI-22 참고**: 기존 코드는 `Files.userSelected`가 취소 시 `FileNotFoundException`을 던지고, 이것이 `IOException`으로 잡혀 취소해도 `"Read Failed!"`가 뜬다. 리팩토링이므로 이 동작을 그대로 유지했다. 고칠지는 별도 이슈로 다룬다(AGENTS.md 3-2 강령 2).

## 타이머 (`SwingTickSource`)

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| UI-23 | 정상 | 틱이 Swing 이벤트 스레드에서 실행되는지 확인 | `start(10, onTick)` | `onTick` 안에서 `SwingUtilities.isEventDispatchThread()`가 true | `SwingTickSourceTest#ticksOnEventThread` |
| UI-24 | 정상 | `stop()` 후 더 이상 틱이 오지 않는지 확인 | 시작 → 정지 → 대기 | 정지 후 호출 횟수 증가 없음 | `SwingTickSourceTest#stopHaltsTicks` |
| UI-25 | 경계 | 다시 `start`하면 이전 타이머가 남지 않는지 확인 | `start(10)` → `start(10)` | 타이머 하나 분량의 틱만 발생 | `SwingTickSourceTest#restartReplacesTimer` |
| UI-26 | 경계 | 0 이하 간격은 타이머를 만들지 않는지 확인 | `start(0, onTick)` | 틱 없음 | `SwingTickSourceTest#nonPositiveIntervalDoesNotTick` |

## 수동 시연 (자동화하지 않는 항목)

창을 띄워야 확인할 수 있는 항목은 2단계에서 기존 버전과 나란히 캡처한다.
- 셀 클릭 토글
- Go 메뉴 각 속도와 Tick, Halt
- Clear
- Store 후 Load
- 잘못된 파일을 Load했을 때 오류 표시
- 실행 중 메뉴를 열어도 메뉴가 덮이지 않는지 ([004](004-swing-tick-source.md))
- 창 크기 조정
