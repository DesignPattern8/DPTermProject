# 저장 테스트

[EXT-M01~03](../../docs/domain-extraction-tests.md), [기존 CL-M 케이스](../../docs/test-cases.md)를 따른다.

2026-11-04 main 병합 후 #16·#17 계약을 적용한다(#29). 정상 저장은 좌표당 1회, 빈 Collection은 빈 보드, String/null/Point 아닌 원소는 IOException이다. ClassicMementoTest의 입력·기대값은 유지하고 직접 load 호출만 LegacyLifeCodec으로 이전한다. LoadFailureCharacterizationTest의 이전 버그 기대값은 main에서 수정된 계약으로 갱신한다. 실패 시 Clear 순서는 유지한다.

향후 리팩토링·프리셋의 구현 전 케이스는 [002의 C-01~08](002-public-contract-and-refactoring.md#6-진행-순서와-검증)에 정의한다. 현재 테스트와 예정 테스트를 구분한다.
