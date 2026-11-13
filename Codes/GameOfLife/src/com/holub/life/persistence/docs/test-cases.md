# 저장 테스트

[EXT-M01~03](../../docs/domain-extraction-tests.md), [기존 CL-M 케이스](../../docs/test-cases.md)를 따른다.

2026-11-04 main 병합 후 #16·#17 계약을 적용한다(#29). 정상 저장은 좌표당 1회, 빈 Collection은 빈 보드, String/null/Point 아닌 원소는 IOException이다. ClassicMementoTest의 입력·기대값은 유지하고 직접 load 호출만 LegacyLifeCodec으로 이전한다. LoadFailureCharacterizationTest의 이전 버그 기대값은 main에서 수정된 계약으로 갱신한다. 실패 시 Clear 순서는 유지한다.

향후 리팩토링·프리셋의 구현 전 케이스는 [002의 C-01~08](002-public-contract-and-refactoring.md#6-진행-순서와-검증)에 정의한다. 현재 테스트와 예정 테스트를 구분한다.

## 암세포 저장 (PR #60)

암세포가 없는 보드는 기존 `Collection<Point>` 형식(CL-M-04)으로 저장한다. 암세포가 있을 때만 `CancerStateData` 형식으로 저장한다. 불러오기는 두 형식을 모두 읽는다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| CAN-P01 | 정상 | 암세포가 있는 보드를 저장했다가 불러오면 생존·암 위치가 그대로다 | 일반 세포 1칸 + 암세포 2칸 저장 → 새 보드에 불러오기 | 살아 있는 칸 3개, 암세포는 같은 2칸 | `CancerCodecTest#cancerBoardRoundTrips` |
| CAN-P02 | 경계 | 암세포가 없으면 기존 형식으로 저장한다 | 일반 세포 2칸 저장 | 기록된 객체가 `Collection<Point>`이다 | `CancerCodecTest#boardWithoutCancerKeepsLegacyFormat` |
| CAN-P03 | 정상 | 기존 형식 파일은 암세포 없이 불러온다 | `List.of(Point(3,4))` 직렬화 파일 | (행 4, 열 3)이 살아 있고 암세포 0개 | `CancerCodecTest#legacyFileLoadsWithoutCancer` |
| CAN-P04 | 예외 | 암세포 형식에 Point가 아닌 원소가 있으면 IOException이 난다 | 생존 목록에 String이 섞인 상태 저장 → 불러오기 | `IOException` | `CancerCodecTest#cancerFileWithNonPointEntryIsRejected` |
