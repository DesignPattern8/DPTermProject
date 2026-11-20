# 실행 도메인 (B)

Clock은 실행 시점과 속도(`Speed`)를 정하고, 시간 흐름은 `TickSource`(기본 `UtilTimerTickSource` = 기존 `java.util.Timer`)에서 받는다. GameController는 원본 Universe의 tick·편집·파일 작업 순서를 수행하고 보드 변경을 `GameListener`로 알린다(#44). 편집·세대 진행은 A의 `Board`를 통해 한 번에 하나씩 실행한다(#55).

- [설계 결정](001-domain-extraction.md)
- [계약](interfaces.md)
- [클래식 보드 연동 기준](../../model/docs/interfaces.md#후속-작업-인계)
- [테스트](test-cases.md)

- [전체 이전 구조·전후 UML](002-domain-layout.md)
- [기존 패턴 분석과 GoF 리팩토링 (채택, #43·#44)](003-pattern-analysis.md)
- [Board 연결과 보드 호출 직렬화 (채택, #55)](004-board-connection.md)
- [Load 성공 후 적용·실패 상태 보존 (#63)](005-safe-load.md)
- [#63 검증 결과](verification-063.md)
- [검증 결과](verification.md)

- [main 병합 후 CI 복구 (#29)](verification.md#2026-11-04-main-병합-후-ci-복구-29)

- [C 저장·패턴 도메인의 공개 계약 및 도메인 간 변경 제안](../../persistence/docs/002-public-contract-and-refactoring.md)

- [C 파일 API·모델 값 연동 (#61)](../../persistence/docs/003-file-api-and-model-values.md): 기존 Memento 저장 경로·암세포 보존, 클래식 Snapshot 변환과 C/B 책임 경계.

- [C Legacy 입력 허용 타입·자원 한도 (#65)](../../persistence/docs/005-legacy-input-policy.md): 지원하지 않는 타입·초과 입력은 IOException. B의 Load 실패 후 보드·Clock 정책은 #63에서 별도로 변경한다.
- [C 안전한 Store 계약 (#64)](../../persistence/docs/004-atomic-file-replacement.md): capture는 임시 파일 준비 후 호출하며 저장 실패 시 기존 파일을 보존한다. Clock 정지·Store 무알림 계약은 유지한다.
- [C 스냅샷 파일 API·블록 순서 (#92)](../../persistence/docs/006-snapshot-file-api.md): 크기를 받는 read와 writeSnapshot, 조립 시 블록 폭 설정을 제공한다. B의 파일용 Cell·PersistenceMapper 제거와 연결은 #88에서 진행한다.
