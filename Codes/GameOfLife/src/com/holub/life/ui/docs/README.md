# ui 도메인

기존 그리기·픽셀 좌표 처리. #24의 기존 메서드 추출이며 새로운 패턴은 추가하지 않는다.

- [설계 결정](001-domain-extraction.md)
- [계약](interfaces.md)
- [클래식 보드 연동 기준](../../model/docs/interfaces.md#후속-작업-인계)
- [테스트](test-cases.md)

- [전체 이전 구조·전후 UML](../../application/docs/002-domain-layout.md)
- [검증 결과](../../application/docs/verification.md)

- [C 저장·패턴 도메인의 공개 계약 및 도메인 간 변경 제안](../../persistence/docs/002-public-contract-and-refactoring.md)
- B 리팩토링(#44)으로 `GameMenu`·`BoardPanel`의 화면 갱신 호출이 `GameListener` 구독 한 곳으로 모였다: [application/003](../../application/docs/003-pattern-analysis.md#8-1-구현-결과-44)
