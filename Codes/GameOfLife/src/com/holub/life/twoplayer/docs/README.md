# 2인용 모드 (`com.holub.life.twoplayer`)

두 플레이어가 한 화면에서 번갈아 셀을 두고, 클래식 B3/S23 규칙으로 영역을 다투는 승패 모드다. 규칙은 [rules.md](rules.md).

## 현재 상태 (2026-11-16)

| 부분 | 이슈 | 상태 |
| --- | --- | --- |
| 게임 진행 모델 (단계·차례·배치·패스·승패 판정) | #6 | 구현 완료, 보드는 대역으로 테스트 |
| 셀 상태(DEAD/A/B)와 세대 전이 규칙, `TwoPlayerBoard` 실제 구현 | #5 | 구현 완료([006](006-two-player-cell.md) 후보 F, model·ui·application 무수정). `NeighborhoodTwoPlayerBoard`로 TP-R·TP-B·TP-I 37개 통과 |
| UI 통합 (모드 선택·입력·상태 표시줄) | #7 | 구현 완료([007](007-two-player-ui.md) D: `GameMode` + `ModeSwitch`, model·application 무수정). TP-U 17개 통과 |
| 문서·다이어그램 정리 | #8 | 완료. 유스케이스 다이어그램 추가, 클래스·시퀀스·상태 다이어그램을 현재 코드로 갱신, rules.md 9장(구현과 다른 부분) |

브랜치: 이슈마다 `feature/base`에서 브랜치를 따서(`feature/5-two-player-cell`, `feature/7-two-player-ui`) `feature/base`로 PR을 보낸다. 예전 통합 브랜치 `feature/two-player-mode`는 2026-11-15에 지웠다.

## 클래스

| 타입 | 공개 | 역할 |
| --- | --- | --- |
| `TwoPlayerGame` | public | 게임 진행. 입력(`select`·`confirm`·`pass`·`tick`)을 받아 현재 단계에 위임 |
| `TwoPlayerBoard` | public interface | 게임 진행이 보드에 묻는 것(크기, 빈 칸, 놓기, 세대 진행, 셀 수). 셀 상태를 숨긴다 |
| `GameSettings` | public | N, K, M, R (기본 20, 20, 3, 5) |
| `Player`, `PhaseKind`, `Response`, `Coordinate` | public | 플레이어, 단계 종류, 입력 결과(거부 이유), 칸 좌표 |
| `GameResult`, `Outcome`, `EndReason` | public | 게임 결과 |
| `Phase` | package-private interface | 단계 (State 패턴) |
| `PlacingPhase`, `InitialPlacementPhase`, `InterventionPhase` | package-private | 칸을 고르고 확정하는 단계 |
| `SimulationPhase`, `GameOverPhase` | package-private | 세대 진행·판정, 종료 |
| `GameJudge` | package-private | 판정 규칙 (셀 수 → 결과) |
| `NeighborhoodTwoPlayerBoard` | public | `TwoPlayerBoard` 실제 구현. 64×64 `Neighborhood(8, Neighborhood(8, TwoPlayerResident))`. 칸 소유자 조회 `owner(row, column)` (#5, 006) |
| `TwoPlayerResident` | package-private | 2인용 셀(`Cell` 구현). 상태는 소유자 `Player`(null = DEAD), rules.md 3장 전이 규칙 |
| `TwoPlayerSession` | public | UI 진입점의 모델 쪽(Facade). 보드·게임을 만들고 클릭을 현재 플레이어 입력으로 바꾼다. 상태 표시줄 문구, 그리기용 스냅숏, 리스너(Observer). `synchronized` (#7, 007) |
| `ui.TwoPlayerMode` | public | 2인용 진입점. ui의 `GameMode` 구현으로 Mode 메뉴에 붙는다 (#7) |
| `ui.TwoPlayerPanel` | package-private | 보드 + 상태 표시줄 + 확정·패스·새 게임 버튼, Enter = 확정 (#7) |
| `ui.TwoPlayerRenderer` | package-private | 스냅숏을 그린다. A·B 색, 진영 경계선, 임시 배치 테두리. `Neighborhood` 잠금을 쓰지 않음(#74) (#7) |

`com.holub.life.twoplayer`와 모델은 AWT·Swing에 의존하지 않는다(UI 코드는 하위 패키지 `twoplayer.ui`, #7). #5부터 `Cell`·`Neighborhood`를 사용하고, #71에서 `Cell.transfer` 좌표를 기존 `CellPosition(row, column)`으로 바꿔 `TwoPlayerResident`의 Point 의존도 제거했다. 006의 Point 비용은 당시 기록으로 보존한다. 게임 진행 쪽(`TwoPlayerGame`, 단계, `GameJudge`)은 여전히 보드 구현을 모른다. [#71 설계·검증·인계](../../../../../../../../docs/GTY.md#008-모델-좌표-계약의-point-의존-제거-71).

## 문서

**규칙·설계 결정** (번호 순. 001·002는 core 쪽 문서, 아래 끝 참고)

| 문서 | 이슈 | 결정 |
| --- | --- | --- |
| [rules.md](rules.md) | #6 | 규칙 명세 (2026-11-03 결정). 구현과 다른 부분은 9장(D1~D8) |
| [003-game-phase.md](003-game-phase.md) | #6 | 게임 단계 표현 → State |
| [004-player-input.md](004-player-input.md) | #6 | 플레이어 입력 전달 → 직접 호출 + `Response` (Command 기각) |
| [005-cell-count-and-judge.md](005-cell-count-and-judge.md) | #6 | 셀 수 집계와 승패 판정 → 별도 판정 클래스 `GameJudge` |
| [006-two-player-cell.md](006-two-player-cell.md) | #5 | 2인용 셀 상태와 `TwoPlayerBoard` 구현 → twoplayer 안의 셀 + `Neighborhood` 재사용(model 무수정, 후보 F). `CompositeBoard`·`Board`는 쓰지 않음. 001 대체 |
| [007-two-player-ui.md](007-two-player-ui.md) | #7 | UI 통합 → `GameMode` + 시계 하나를 잠금 하나로 나누는 `ModeSwitch`(Strategy), 진입점 `TwoPlayerSession`(Facade), 상태 표시줄 Observer, 렌더러는 `twoplayer.ui` 안. #74 모드 전환 재현 |
| [interfaces.md](interfaces.md) | #5·#7 | `TwoPlayerBoard`, `NeighborhoodTwoPlayerBoard`, `TwoPlayerGame`, `TwoPlayerSession`, `TwoPlayerMode` 시그니처와 계약 |
| [test-cases.md](test-cases.md) | #5·#6·#7 | 테스트 케이스 정의 (TP-R·TP-B·TP-I·TP-F·TP-P·TP-E·TP-U), 아래 "테스트" 요약 |

**다이어그램** (`diagrams/`, `.puml` 원본 + png·svg)

| 다이어그램 | 내용 |
| --- | --- |
| [twoplayer-usecase](diagrams/twoplayer-usecase.puml) ([png](diagrams/twoplayer-usecase.png), [svg](diagrams/twoplayer-usecase.svg)) | 유스케이스 (구현 기준, 확장은 `E` 접두). 원본 초안 `diagrams/use-case diagram/holub-life-usecase-2p.puml`(저장소 루트, 수정하지 않음)과 달라진 점은 `.puml` 머리 주석 |
| [twoplayer-class](diagrams/twoplayer-class.puml) ([png](diagrams/twoplayer-class.png), [svg](diagrams/twoplayer-class.svg)) | `com.holub.life.twoplayer` 클래스 (게임 진행 모델, 실제 보드, `TwoPlayerSession`). 공개 타입 흰색, package-private 회색 |
| [twoplayer-ui-before](diagrams/twoplayer-ui-before.puml) ([png](diagrams/twoplayer-ui-before.png), [svg](diagrams/twoplayer-ui-before.svg)) / [twoplayer-ui-class](diagrams/twoplayer-ui-class.puml) ([png](diagrams/twoplayer-ui-class.png), [svg](diagrams/twoplayer-ui-class.svg)) | UI 통합 전/후 클래스 (#7, `twoplayer.ui`·`ui.ModeSwitch`) |
| [twoplayer-game-phase-state](diagrams/twoplayer-game-phase-state.puml) ([png](diagrams/twoplayer-game-phase-state.png), [svg](diagrams/twoplayer-game-phase-state.svg)) | 게임 단계 상태 (`PhaseKind`), New Game |
| [twoplayer-round-sequence](diagrams/twoplayer-round-sequence.puml) ([png](diagrams/twoplayer-round-sequence.png), [svg](diagrams/twoplayer-round-sequence.svg)) | 한 라운드(시뮬레이션 K세대 → 개입 → 다음 라운드). 시계·패널 → `TwoPlayerSession` → `TwoPlayerGame` 실제 호출 경로 |

셀 상태 표현의 이전 제안(Cell 인터페이스 변경, 006으로 대체됨)은 core 쪽 문서에 기록으로 남아 있다: [../../docs/001-cell-state-representation.md](../../docs/001-cell-state-representation.md), [../../docs/interfaces.md](../../docs/interfaces.md)

## 테스트

`Codes/GameOfLife/test/com/holub/life/twoplayer/`. 정의는 [test-cases.md](test-cases.md).

**접두어별 요약** (2026-11-16, `./gradlew test --rerun-tasks` 결과: 2인용 테스트 96개 통과, 실패·건너뜀 0. 저장소 전체 240개 통과)

| 접두어 | 내용 | 명세 | 이슈 | 테스트 클래스 | 정의 | JUnit |
| --- | --- | --- | --- | --- | --- | --- |
| TP-R | 세대 전이 규칙 (실제 보드) | 3장 | #5 | `TwoPlayerRuleTest` | 18 | 17 |
| TP-B | `TwoPlayerBoard` 계약 (실제 보드) | — | #5 | `NeighborhoodTwoPlayerBoardTest` | 17 | 17 |
| TP-I | 게임 진행을 실제 보드로 다시 확인 | 4·6장 | #5 | `TwoPlayerGameOnRealBoardTest` | 3 | 3 |
| TP-F | 게임 진행 (대역 보드) | 4장 | #6 | `TwoPlayerGameFlowTest` | 17 | 17 |
| TP-P | 배치 규칙 (대역 보드) | 5장 | #6 | `PlacementRuleTest` | 15 | 15 |
| TP-E | 종료·승패 판정 | 6장 | #6 | `GameJudgeTest` | 10 | 10 |
| TP-U | UI 통합 (headless, 대역 모드·대역 `TickSource`·`BufferedImage`) | 4·5·8장 | #7 | `ui.TwoPlayerUiTest` | 17 | 17 |
| **합계** | | | | | **97** | **96** |

- TP-R-18(클래식 모드가 그대로 동작)은 따로 메서드를 두지 않고 기존 클래식·core 테스트 클래스로 확인한다. 그래서 정의가 JUnit보다 1개 많다.

**테스트 도우미·측정 클래스** (`@Test` 없음)

| 클래스 | 역할 |
| --- | --- |
| `StubBoard`, `GameScript`, `RealBoardScript` | 대역 보드, 게임 진행 도우미, 실제 보드 도우미 |
| `ui.StaticLockCheck` | 007 "#74 확인" 측정 (`main`으로 실행) |
| `CellCandidateBenchmark` | 006의 측정·동등성 확인. F는 실제 `NeighborhoodTwoPlayerBoard` (`main`으로 실행, 방법은 006 "측정 방법") |

실행: `cd Codes/GameOfLife && ./gradlew test` (JDK 26)

다이어그램 렌더링: `cd Codes/GameOfLife && ./gradlew renderDiagrams -Pdir=src/com/holub/life/twoplayer` (원본 `.puml`과 png·svg를 함께 커밋한다, AGENTS.md 2-5)
