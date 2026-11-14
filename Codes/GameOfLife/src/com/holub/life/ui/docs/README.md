# ui 도메인

화면(그리기·픽셀 좌표)과 사용자 입력(마우스, Grid·Go 메뉴, 파일 대화상자). 담당 이슈 #9.

## 설계 결정

| 문서 | 상태 | 내용 |
| --- | --- | --- |
| [001-domain-extraction](001-domain-extraction.md) | 채택 | #24/#25: 모델·`Universe`에서 화면 코드를 `ui`로 추출 |
| [001-screen-update](001-screen-update.md) | 대체됨(→ application/003 D4) | #25 이전 초안. 화면 갱신 Observer |
| [002-board-rendering](002-board-rendering.md) | 대체됨(→ 007) | #25 이전 초안. 렌더링 Decorator |
| [003-input-binding](003-input-binding.md) | 대체됨(→ 005) | #25 이전 초안. 메뉴 연결 |
| [004-swing-tick-source](004-swing-tick-source.md) | 보류 | Swing Timer. 스레드가 바뀌어 B와 별도 이슈로 |
| [005-menu-binding](005-menu-binding.md) | 채택 | 메뉴를 (메뉴, 라벨, 동작) 항목 표로 |
| [006-dialog-injection](006-dialog-injection.md) | 채택 | 파일 선택·오류 표시·종료를 `UserDialogs`(Strategy)로 |
| [007-renderer-dispatch](007-renderer-dispatch.md) | 채택(현행 유지) | 렌더러 셀 타입 분기. 008에서 재검토 후 유지 |
| [008-board-api-boundary](008-board-api-boundary.md) | 채택 | A의 Board API: 클릭 경계만 `CellPosition`, 렌더러는 트리 유지 |
| [009-game-result-dialog](009-game-result-dialog.md) | 채택 | 암세포 게임 결과 메시지를 `UserDialogs.showMessage` + `GameResultAnnouncer`로 |

001-screen-update~004는 #25 이전 구조(`68ad5e6`)에서 쓴 초안이다. 번호가 001-domain-extraction과 겹치지만 AGENTS.md 2-3에 따라 지우지 않고 상태만 바꿨다. 초안 코드(`ActiveBlockDecorator` 등 4개)는 #45 병합 커밋에서 제거했고 커밋 `4eeaa8b`에 남아 있다.

- 다이어그램: [005-before](diagrams/005-before.puml) / [005-after](diagrams/005-after.puml), [009-before](diagrams/009-before.puml) / [009-after](diagrams/009-after.puml)

### 암세포 기능(#60)이 바꾼 화면 동작

| 입력·알림 | 처리 | 테스트 |
| --- | --- | --- |
| 왼쪽 클릭 | `GameController.toggle(row, column)` (기존) | UI-R13, UI-R15 |
| 오른쪽 클릭 | `GameController.treat(row, column)`: 그 칸과 주변 8칸 치료, 치료 횟수 1 사용 | UI-R14 |
| 암세포 칸 | 마젠타 (생존 빨강, 빈 칸 연노랑) | UI-R16 |
| `GameListener.Change.GAME_OVER` | `GameResultAnnouncer` → `UserDialogs.showMessage` (이벤트 스레드) | UI-R17~R19 |
| `GENERATION` / `EDITED` | `BoardPanel`의 부분 갱신 / 전체 다시 그리기 (기존) | EXT-R01~05 |

## 그 밖의 문서

- [계약](interfaces.md)
- [클래식 보드 연동 기준](../../model/docs/interfaces.md#후속-작업-인계)
- [테스트](test-cases.md)

- [전체 이전 구조·전후 UML](../../application/docs/002-domain-layout.md)
- [검증 결과](../../application/docs/verification.md)

- [C 저장·패턴 도메인의 공개 계약 및 도메인 간 변경 제안](../../persistence/docs/002-public-contract-and-refactoring.md)
- B 리팩토링(#44)으로 `GameMenu`·`BoardPanel`의 화면 갱신 호출이 `GameListener` 구독 한 곳으로 모였다: [application/003](../../application/docs/003-pattern-analysis.md#8-1-구현-결과-44)
