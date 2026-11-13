# 실행 도메인 (B)

Clock은 실행 시점과 속도(`Speed`)를 정하고, 시간 흐름은 `TickSource`(기본 `UtilTimerTickSource` = 기존 `java.util.Timer`)에서 받는다. GameController는 원본 Universe의 tick·편집·파일 작업 순서를 수행하고 보드 변경을 `GameListener`로 알린다(#44).

- [설계 결정](001-domain-extraction.md)
- [계약](interfaces.md)
- [클래식 보드 연동 기준](../../model/docs/interfaces.md#후속-작업-인계)
- [테스트](test-cases.md)

- [전체 이전 구조·전후 UML](002-domain-layout.md)
- [기존 패턴 분석과 GoF 리팩토링 (채택, #43·#44)](003-pattern-analysis.md)
- [검증 결과](verification.md)

- [main 병합 후 CI 복구 (#29)](verification.md#2026-11-04-main-병합-후-ci-복구-29)

- [C 저장·패턴 도메인의 공개 계약 및 도메인 간 변경 제안](../../persistence/docs/002-public-contract-and-refactoring.md)
