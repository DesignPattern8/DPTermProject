# 2인용 모드 (`com.holub.life.twoplayer`)

두 플레이어가 한 화면에서 번갈아 셀을 두고, 클래식 B3/S23 규칙으로 영역을 다투는 승패 모드다. 규칙은 [rules.md](rules.md).

## 현재 상태 (2026-11-15)

| 부분 | 이슈 | 상태 |
| --- | --- | --- |
| 게임 진행 모델 (단계·차례·배치·패스·승패 판정) | #6 | 구현 완료, 보드는 대역으로 테스트 |
| 셀 상태(DEAD/A/B)와 세대 전이 규칙, `TwoPlayerBoard` 실제 구현 | #5 | 구현 완료([006](006-two-player-cell.md) 후보 F, model·ui·application 무수정). `NeighborhoodTwoPlayerBoard`로 TP-R·TP-B·TP-I 37개 통과 |
| UI 통합 (모드 선택·입력·상태 표시줄) | #7 | 대기 |
| 문서·다이어그램 정리 | #8 | 대기 |

브랜치: `feature/two-player-mode`가 2인용 통합 브랜치다. 단계마다 여기서 브랜치를 따서 PR을 보내고, `main`으로는 #5까지 끝난 뒤 한 번에 PR을 연다.

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

Swing에 의존하지 않는다. #5부터 `com.holub.life.model`의 `Cell`·`Neighborhood`를 쓰고(model은 고치지 않음), `Cell.transfer` 시그니처 때문에 `TwoPlayerResident`가 `java.awt.Point`를 import한다(006 감수하는 비용, 이슈 초안 [006c](../../../../../../../../docs/issue-drafts/006c-model-api-exposes-awt-point.md)). 게임 진행 쪽(`TwoPlayerGame`, 단계, `GameJudge`)은 여전히 보드 구현을 모른다.

## 문서

| 문서 | 내용 |
| --- | --- |
| [rules.md](rules.md) | 규칙 명세 (2026-11-03 결정 반영) |
| [003-game-phase.md](003-game-phase.md) | 설계 결정: 게임 단계 표현 → State |
| [004-player-input.md](004-player-input.md) | 설계 결정: 플레이어 입력 전달 → 직접 호출 + `Response` |
| [005-cell-count-and-judge.md](005-cell-count-and-judge.md) | 설계 결정: 셀 수 집계와 승패 판정 → 별도 판정 클래스 |
| [006-two-player-cell.md](006-two-player-cell.md) | 설계 결정: 2인용 셀 상태와 `TwoPlayerBoard` 구현 → twoplayer 안의 셀 + `Neighborhood` 재사용(model 무수정), `CompositeBoard`·`Board`는 쓰지 않음. 001 대체 |
| [interfaces.md](interfaces.md) | `TwoPlayerBoard`, `TwoPlayerGame` 시그니처와 계약 |
| [test-cases.md](test-cases.md) | 테스트 케이스 정의 (TP-R·TP-F·TP-P·TP-E·TP-U) |
| [diagrams/twoplayer-game-phase-state.puml](diagrams/twoplayer-game-phase-state.puml) ([png](diagrams/twoplayer-game-phase-state.png), [svg](diagrams/twoplayer-game-phase-state.svg)) | 게임 단계 상태 다이어그램 |
| [diagrams/twoplayer-class.puml](diagrams/twoplayer-class.puml) ([png](diagrams/twoplayer-class.png), [svg](diagrams/twoplayer-class.svg)) | 게임 진행 모델 클래스 다이어그램 (공개 타입 흰색, package-private 회색) |
| [diagrams/twoplayer-round-sequence.puml](diagrams/twoplayer-round-sequence.puml) ([png](diagrams/twoplayer-round-sequence.png), [svg](diagrams/twoplayer-round-sequence.svg)) | 한 라운드(시뮬레이션 K세대 → 개입 → 다음 라운드) 시퀀스 다이어그램 |

셀 상태 표현의 이전 제안(Cell 인터페이스 변경, 006으로 대체됨)은 core 쪽 문서에 기록으로 남아 있다: [../../docs/001-cell-state-representation.md](../../docs/001-cell-state-representation.md), [../../docs/interfaces.md](../../docs/interfaces.md)

## 테스트

`Codes/GameOfLife/test/com/holub/life/twoplayer/`

| 클래스 | 케이스 | 수 |
| --- | --- | --- |
| `TwoPlayerGameFlowTest` | TP-F 게임 진행 | 17 |
| `PlacementRuleTest` | TP-P 배치 규칙 | 15 |
| `GameJudgeTest` | TP-E 종료·승패 판정 | 10 |
| `TwoPlayerRuleTest` | TP-R 세대 전이 규칙 (실제 보드) | 17 |
| `NeighborhoodTwoPlayerBoardTest` | TP-B `TwoPlayerBoard` 계약 (실제 보드) | 17 |
| `TwoPlayerGameOnRealBoardTest` | TP-I 게임 진행을 실제 보드로 다시 확인 | 3 |
| `StubBoard`, `GameScript`, `RealBoardScript` | 대역 보드, 게임 진행 도우미, 실제 보드 도우미 | — |
| `CellCandidateBenchmark` | 006의 측정·동등성 확인. F는 실제 `NeighborhoodTwoPlayerBoard` (`main`으로 실행, `@Test` 없음. 실행 방법은 006 "측정 방법") | — |

실행: `cd Codes/GameOfLife && ./gradlew test` (JDK 26)

다이어그램 렌더링: `cd Codes/GameOfLife && ./gradlew renderDiagrams -Pdir=src/com/holub/life/twoplayer` (원본 `.puml`과 png·svg를 함께 커밋한다, AGENTS.md 2-5)
