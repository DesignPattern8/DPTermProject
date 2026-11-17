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
| C-13 | 예외 | Controller Load 실패 보존 (#63에서 계약 변경) | 개방 실패/해석 실패 | 보드·Clock 유지, Load 알림 없음 | `GameControllerTest`, `GameControllerBoardTest` 및 `GameControllerPersistenceTest#preservesStateOnOpenAndDecodeFailure` |
| C-14 | 예외 | 프리셋 필드 계약 | null id/표시명/데이터 | NullPointerException | `PresetCatalogTest#rejectsNullPresetFields` |

## #65 역직렬화 정책 — 구현 전 정의 (2026-11-16)

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| FIL-01 | 정상 | 초기 저장기 호환 | e38ce7a flush 고정 fixture | 순서·중복·음수 좌표 유지 | `LegacyInputPolicyTest#readsOriginalWriterFixture` |
| FIL-02 | 정상/경계 | 허용 컬렉션 호환 | LinkedList/ArrayList/HashSet/Arrays/List.of/빈·단일 목록 | Point 값 동일 | `LegacyInputPolicyTest#readsSupportedCollections` |
| FIL-03 | 예외 | 임의 객체 hook 실행 전 거부 | readObject 표식 객체가 든 LinkedList | IOException, 표식 미실행 | `LegacyInputPolicyTest#rejectsUnknownClassBeforeReadObject` |
| FIL-04 | 예외 | Point 하위 클래스 거부 | readObject 표식 Point subclass | IOException, 표식 미실행 | `LegacyInputPolicyTest#rejectsPointSubclassBeforeReadObject` |
| FIL-05 | 예외 | 미지원 컬렉션 거부 | Vector<Point> | InvalidClassException | `LegacyInputPolicyTest#rejectsUnsupportedCollection` |
| FIL-06 | 경계 | 배열 할당 한도 | Object[] 32,768 / 32,769 | 전자는 의미 검사까지, 후자는 filter 거부 | `LegacyInputPolicyTest#boundsArrayAllocation` |
| FIL-07 | 경계 | 깊이 한도 | Point를 감싼 ArrayList 깊이 8 / 9 | 전자는 의미 검사까지, 후자는 filter 거부 | `LegacyInputPolicyTest#boundsGraphDepth` |
| FIL-08 | 경계 | 참조 수 한도 | 동일 Point 반복 목록, 측정된 refs 경계 | 경계까지 의미 검사, 초과 filter 거부 | `LegacyInputPolicyTest#boundsRepeatedReferences` |
| FIL-09 | 경계 | 소비 바이트 한도 | reset padding으로 크기 1 MiB / +1인 빈 legacy 목록 | 정확히 한도는 성공, +1은 IOException | `LegacyInputPolicyTest#boundsBytesConsumed` |
| FIL-10 | 예외 | 필터 밖 문자열 제한 | 1 MiB를 넘는 serialized String | IOException, 읽은 바이트 ≤1 MiB, stream 열림 | `LegacyInputPolicyTest#boundsUnfilteredStringsWithoutClosingInput` |
| FIL-11 | 정상/경계 | 최대 출력 왕복 | 생존/암 각각 16,384 좌표 | 같은 PatternData 복원 | `LegacyInputPolicyTest#roundTripsLargestSupportedCancerPattern` |
| FIL-12 | 예외 | 원소 한도 | LinkedList 16,385 Point / 초과 PatternData | read IOException / write 전 거부·출력 비변경 | `LegacyInputPolicyTest#rejectsOversizedCellCollections` |
| FIL-13 | 예외 | JVM 제한 유지 | 별도 JVM 전역 Point 거부 필터 | Codec도 거부, 전역 필터 완화 없음 | `LegacyInputPolicyTest#preservesJvmWideFilter` |
| FIL-14 | 정상/경계 | stream 계약 유지 | 정상 첫 객체 뒤 큰 데이터 | 첫 객체만 읽기, caller stream 열림 | `LegacyInputPolicyTest#leavesTrailingDataAndStreamOwnershipToCaller` |
| FIL-15 | 예외 | skip도 바이트 한도 적용 | 거대한 primitive 클래스 annotation | IOException, 실제 소비 ≤1 MiB | `LegacyInputPolicyTest#countsSkippedClassAnnotationsAgainstByteLimit` |

기존 CL-M/CAN-P/C-01~14 및 고정 암세포 fixture 테스트는 유지한다. readObject 표식은 정수 카운터만 변경하는 무해한 테스트 객체다.
## #64 안전한 Store — 구현 전 정의 (2026-11-16)

정상/경계/예외 기대를 먼저 정의하고 아래 회귀 테스트를 작성한다. 기존 C/APP/CAN 테스트는 유지한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| SAFE-01 | 예외 | capture 실패 시 원본 보존 | 기존 파일 + 예외 Supplier | 동일 바이트·파일 목록, 동일 예외 | `SafeBoardFileStoreTest#captureFailurePreservesExistingFile` |
| SAFE-02 | 예외 | 부분 기록 실패 시 원본 보존 | 일부 기록 후 IOException Codec | 원본 바이트 유지·임시 파일 없음 | `SafeBoardFileStoreTest#partialWriteFailurePreservesExistingFile` |
| SAFE-03 | 경계/예외 | 새 경로 실패 시 찌꺼기 제거 | 없는 대상 + 기록 실패 | 대상/임시 파일 모두 없음 | `SafeBoardFileStoreTest#failedNewSaveLeavesNoFiles` |
| SAFE-04 | 정상 | 완성 전 원본 비변경 | Codec 기록 중 원본 읽기 | 원본 바이트를 계속 읽을 수 있음 | `SafeBoardFileStoreTest#targetRemainsReadableWhileEncoding` |
| SAFE-05 | 정상 | 기존/새 파일 저장 성공 | 기존 파일과 없는 경로에 PatternData | 같은 패턴 복원·임시 파일 없음 | `SafeBoardFileStoreTest#successfulSaveReplacesOrCreatesOnlyTarget` |
| SAFE-06 | 경계 | 기존 POSIX 권한 보존 | rw-r----- 파일 | 교체 후 같은 rwx 권한 | `SafeBoardFileStoreTest#preservesExistingPosixPermissions` |
| SAFE-07 | 예외 | 링크 대상 거부 | 일반/깨진 symbolic link | capture 미호출·링크/원본 유지 | `SafeBoardFileStoreTest#rejectsSymbolicLinksBeforeCapture` |
| SAFE-08 | 예외 | 잘못된 경로 준비 실패 | 디렉터리/부모 없음/부모가 파일 | capture 미호출·원본 유지 | `SafeBoardFileStoreTest#rejectsInvalidTargetsBeforeCapture` |
| SAFE-09 | 예외 | 읽기 전용 파일 덮어쓰기 방지 | POSIX r--r----- 파일 | capture 미호출·내용/권한 유지 | `SafeBoardFileStoreTest#rejectsReadOnlyTargetBeforeCapture` |
| SAFE-10 | 예외 | 이동 실패 원본 보존 | 실패 이동 대역 + 기존/없는 대상 | 이전 상태 유지·임시 파일 삭제 | `AtomicCommitTest#moveFailurePreservesTargetAndCleansTemporaryFile` |
| SAFE-11 | 예외 | 비원자적 fallback 금지 | AtomicMoveNotSupportedException 대역 | 같은 예외·원본 보존·이동 1회 | `AtomicCommitTest#unsupportedAtomicMoveDoesNotRetry` |
| SAFE-12 | 정상 | 같은 폴더와 stream 종료 확인 | 이동 직전 임시 파일/stream 확인 | 형제 경로·완성 바이트·닫힌 stream | `AtomicCommitTest#commitsCompletedSiblingAfterStreamClosed` |
| SAFE-13 | 예외 | 정리 실패도 보고 | 이동 실패 후 삭제 불가 임시 경로 | 원래 예외 + suppressed 삭제 예외 | `AtomicCommitTest#cleanupFailureIsSuppressedOnOriginalFailure` |
| SAFE-14 | 예외 | B 연동에서 원본·모델 유지 | 암세포/일반 셀 보드 + 실패 Codec | 파일·셀 상태 유지, Clock 정지, 알림 없음 | `GameControllerPersistenceTest#failedStorePreservesFileAndBoard` |

POSIX 권한·심볼릭 링크 테스트는 해당 파일 시스템 기능이 있을 때 실행한다. 임시 테스트 자원은 @TempDir 또는 finally에서 정리한다.
