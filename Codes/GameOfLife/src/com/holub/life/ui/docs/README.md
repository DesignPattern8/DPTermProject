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
| [007-renderer-dispatch](007-renderer-dispatch.md) | 채택(현행 유지) | 렌더러 셀 타입 분기. A의 `BoardSnapshot` 이후 재검토 |

001-screen-update~004는 #25 이전 구조(`68ad5e6`)에서 쓴 초안이다. 번호가 001-domain-extraction과 겹치지만 AGENTS.md 2-3에 따라 지우지 않고 상태만 바꿨다. 초안 코드(`ActiveBlockDecorator` 등 4개)는 #45 병합 커밋에서 제거했고 커밋 `4eeaa8b`에 남아 있다.

- 다이어그램: [005-before](diagrams/005-before.puml) / [005-after](diagrams/005-after.puml)

## 그 밖의 문서

- [계약](interfaces.md)
- [테스트](test-cases.md)

- [전체 이전 구조·전후 UML](../../application/docs/002-domain-layout.md)
- [검증 결과](../../application/docs/verification.md)

- B 리팩토링(#44)으로 `GameMenu`·`BoardPanel`의 화면 갱신 호출이 `GameListener` 구독 한 곳으로 모였다: [application/003](../../application/docs/003-pattern-analysis.md#8-1-구현-결과-44)
