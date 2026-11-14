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

## #61 구현 전 확정 케이스 (2026-11-14)

아래 정의 후 테스트를 작성하고 구현한다. 기존 CL-M/CAN-P/APP 테스트는 입력·기대값을 보존한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| C-01 | 정상 | 좌표·legacy 순회 순서 왕복 | 비대칭 좌표 목록 | Point x=column/y=row, 순서 동일 | `LegacyLifeCodecTest#roundTripsLegacyCoordinates` |
| C-02 | 경계 | 빈·중복·순서 보존 | 빈 목록, 중복 좌표 | 목록 그대로 복원 | `LegacyLifeCodecTest#preservesEmptyAndDuplicateEntries` |
| C-03 | 예외 | 잘못된 파일 거부 | String/null/비Point/깨진 stream | IOException | `LegacyLifeCodecTest#rejectsMalformedContents` |
| C-04 | 경계 | 보드 적용 정책 분리 | 음수·큰 값·중복 | Codec 보존, 클래식 snapshot은 유효 집합 | `PatternDataTest#clipsAndDeduplicatesOnlyWhenCreatingSnapshot` |
| C-05a | 정상/예외 | Codec stream 비소유 | 성공/실패 stream | 닫지 않음 | `LegacyLifeCodecTest#doesNotCloseCallerStreams` |
| C-05b | 정상/예외 | Facade stream 종료 | 성공/실패 Codec 대역 | 전달된 실제 File stream이 닫힘 | `BoardFileStoreTest#closesStreamsOnSuccessAndFailure` |
| C-05c | 예외 | 개방 실패 순서 | 없는 입력/디렉터리 출력 | 준비·캡처 콜백 미호출 | `BoardFileStoreTest#doesNotPrepareWhenOpenFails` |
| C-05d | 예외 | 콜백 실패 자원 정리 | 준비/캡처 예외 | 원래 예외 전파, descriptor 증가 없음 | `BoardFileStoreTest#closesStreamsWhenPreparationFails` |
| C-06 | 경계/예외 | 불변 데이터 | mutable 목록·집합, null | 방어 복사, null 거부 | `PatternDataTest#copiesInputCells`, `PatternDataTest#rejectsNullValues` |
| C-07 | 정상/경계 | 내장 프리셋 값 | block/blinker/glider 조회 | 상대 좌표·순서 고정, 목록 수정 불가 | `PresetCatalogTest#returnsStableImmutablePresets` |
| C-08 | 예외 | 없는 프리셋 | 알 수 없는 id | Optional.empty | `PresetCatalogTest#returnsEmptyForUnknownId` |
| C-09 | 정상 | 이전 암세포 직렬화 호환 | 변경 전 Codec 생성 fixture | 생존·암 위치 복원 | `LegacyLifeCodecTest#readsPreRefactoringCancerFixture` |
| C-10 | 예외 | 암세포 손실 변환 방지 | 암세포가 있는 PatternData | classic snapshot 변환 거부 | `PatternDataTest#rejectsLossyCancerSnapshot` |
| C-11 | 정상/경계 | 모델 연결과 방어 복사 | Memento의 Point 변경, 중복/범위 밖 입력 | 독립 값, Board.restore 결과 일치 | `PersistenceMapperTest#copiesMutableMemento`, `PersistenceMapperTest#restoresClassicSnapshot` |
| C-12 | 정상 | Controller 저장 순회·암세포 유지 | 여러 블록의 일반/암세포 | 기존 순서, 암 상태 왕복 | `GameControllerPersistenceTest#preservesTraversalAndCancer` |
| C-13 | 예외 | Controller 기존 실패/Clock 정책 | 개방 실패/해석 실패 | 기존 보드·Clock·알림 정책 | 기존 `GameControllerTest`, `GameControllerBoardTest` 및 `GameControllerPersistenceTest#preservesOpenAndDecodeFailureOrder` |
| C-14 | 예외 | 프리셋 필드 계약 | null id/표시명/데이터 | NullPointerException | `PresetCatalogTest#rejectsNullPresetFields` |
