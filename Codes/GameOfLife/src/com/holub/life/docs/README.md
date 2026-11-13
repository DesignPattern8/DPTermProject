# Life 도메인 안내

기존 동작을 유지한 #24의 책임 추출 결과다. 실행 클래스는 `com.holub.life.Life`, 소스 루트는 `src`, 테스트 루트는 `test`를 유지한다.

| 담당 | 코드 | 문서 |
| --- | --- | --- |
| A | Cell, Resident, Neighborhood, Direction, NeighborhoodState | [model](../model/docs/README.md) |
| B | Clock, GameController | [application](../application/docs/README.md) |
| C | LegacyLifeCodec, BoardFileStore | [persistence](../persistence/docs/README.md) |
| D | BoardPanel, BoardRenderer, GridCoordinateMapper, GameMenu, FileDialogs, LifeFrame | [ui](../ui/docs/README.md) |

- [전체 구조·이전표·비용](../application/docs/002-domain-layout.md)
- [기존 동작 정의](test-cases.md)
- [추출 전 추가 테스트 정의](domain-extraction-tests.md)
- [실행 검증 결과](../application/docs/verification.md)
- [암세포 기능 변경 요약 (PR #60)](cancer-feature-changes.md)

기존 Composite/Memento/Clock 알림만 유지한다. 새 Board/Strategy/Codec 인터페이스, 불변 스냅샷, Undo/Redo, 프리셋, Swing Timer는 도입하지 않았다. 따라서 `rule`, `pattern`, `command` 빈 패키지도 만들지 않았다.
