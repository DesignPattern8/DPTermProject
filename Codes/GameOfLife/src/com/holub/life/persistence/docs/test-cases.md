# 저장 테스트

구현 전 정의: [EXT-M01~03](../../docs/domain-extraction-tests.md), [기존 CL-M01~09](../../docs/test-cases.md).

기존 ClassicMementoTest의 입력·기대값은 변경하지 않는다. ClassicBoard만 추출한 LegacyLifeCodec 호출로 바꾼다. 중복 저장, ClassCastException, 잘못된 원소 무시와 null 복원 실패를 유지한다.
