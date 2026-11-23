# 저장 테스트

## #92 스냅샷 파일 API — 구현 전 정의 (2026-11-18)

[B에 공유한 계약](https://github.com/DesignPattern8/DPTermProject/issues/88#issuecomment-6953096811)을 기준으로 아래 케이스를 먼저 정의한다. 기존 C/SAFE/FIL 및 B의 순서·좌표 정책 테스트는 수정하지 않는다. B Controller 이전은 #88의 후속 작업이다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| SNAP-P01 | 정상 | 일반·암세포 변환과 원시 데이터 불변 | 혼합 패턴, 직사각형 크기 | 생존·암 집합 및 크기 보존, 원본 목록 불변 | `PatternDataSnapshotTest#preservesCancerAndSourceData` |
| SNAP-P02 | 경계 | 두 집합에 동일한 범위 정책 적용 | 중복, 음수, 크기와 같은 좌표, int 극값, 유효 모서리 | 범위 밖 무시·중복 제거, 유효 암 유지 | `PatternDataSnapshotTest#clipsBothKindsAndDeduplicatesAtBoardBoundary` |
| SNAP-P03 | 경계 | 빈 스냅샷 | 빈 패턴, 1×1·직사각형 | 지정 크기와 빈 두 집합 | `PatternDataSnapshotTest#supportsEmptyBoards` |
| SNAP-P04 | 예외 | 잘못된 크기 | 0·음수 행/열 | IllegalArgumentException | `PatternDataSnapshotTest#rejectsNonPositiveDimensions` |
| SNAP-P05 | 예외 | 생존 집합 밖 암 정보의 조용한 손실 방지 | 범위 안 암 좌표가 생존 목록에 없음 | IllegalArgumentException, 원시 데이터 유지 | `PatternDataSnapshotTest#rejectsCancerOutsideLiveSet` |
| SNAP-F01 | 정상 | 파일 읽기에 대상 크기와 암 정보 전달 | 중복·범위 밖 좌표가 있는 암 파일 | 필터링된 스냅샷, 실제 Board.restore 후 같은 상태 | `SnapshotFileStoreTest#readsFilteredCancerSnapshotIntoBoard` |
| SNAP-F02 | 정상 | 기존 일반/암 파일 호환 | 기존 write로 만든 파일 | 일반·암 스냅샷으로 읽기 | `SnapshotFileStoreTest#readsExistingClassicFile`, 기존 `LegacyLifeCodecTest#readsPreRefactoringCancerFixture` |
| SNAP-F03 | 예외 | 잘못된 크기는 파일 접근 전 거부 | 없는 파일, 잘못된 행/열 | IOException 대신 IllegalArgumentException | `SnapshotFileStoreTest#rejectsInvalidDimensionsBeforeOpening` |
| SNAP-F04 | 예외 | 모델 변환 실패를 파일 오류로 전달 | 생존 집합 밖 암 좌표 파일 | 원인이 IllegalArgumentException인 IOException | `SnapshotFileStoreTest#reportsUnrepresentableCancerAsIoFailure` |
| SNAP-F05 | 정상·예외 | 입력 stream 종료 | 정상·해석 실패·변환 실패 Codec 대역 | 모든 경로에서 실제 stream 닫힘 | `SnapshotFileStoreTest#closesInputOnEveryReadOutcome` |
| SNAP-F06 | 예외 | Codec 예외를 변환 오류로 오인하지 않음 | IOException/IllegalArgumentException을 던지는 Codec | 원래 예외 인스턴스 전파 | `SnapshotFileStoreTest#preservesCodecFailures` |
| SNAP-F07 | 정상·경계 | 평면·중첩 출력 순서를 기존 순회와 비교 | 전체 생존 보드: 64×64 [8,1], 8×8 [4,1]/[1], 12×12 [6,2,1], 1×1 [1] | 직렬화된 목록이 createMemento 순서와 동일 | `SnapshotFileStoreTest#matchesLegacyTraversalForEachLayout` |
| SNAP-F08 | 정상 | 암세포 왕복과 캡처 비변경 | 여러 블록에 걸친 혼합 보드 | 저장·읽기·복원 후 동일 스냅샷, 원본 보드 유지 | `SnapshotFileStoreTest#roundTripsCancerWithoutChangingBoard` |
| SNAP-F09 | 경계 | 빈 보드 저장 | 빈 스냅샷 | 빈 legacy Collection, 재읽기 동일 | `SnapshotFileStoreTest#roundTripsEmptySnapshot` |
| SNAP-F10 | 경계 | 설정 방어 복사 | 생성 후 전달한 폭 목록 변경 | 기존 블록 순서 유지 | `SnapshotFileStoreTest#copiesLayoutBeforeCallerMutation` |
| SNAP-F11 | 예외 | 잘못된 순서 설정 거부 | 빈 목록, 0/음수, 증가/같은 폭, 비약수 단계, 마지막 값 != 1, null 목록/원소 | IllegalArgumentException 또는 NullPointerException | `SnapshotFileStoreTest#rejectsInvalidLayouts`, `SnapshotFileStoreTest#rejectsNullLayouts` |
| SNAP-F12 | 예외 | 설정 없는 새 저장 경로 거부 | 1인자 Store, 기존 파일 | IllegalStateException, capture 미호출, 원본 유지 | `SnapshotFileStoreTest#requiresExplicitLayoutForSnapshotWrites` |
| SNAP-F13 | 예외 | 크기와 설정 불일치 | 행 또는 열이 최상위 블록 폭의 배수가 아님 | IllegalArgumentException, 원본·파일 목록 유지 | `SnapshotFileStoreTest#rejectsIncompatibleDimensionsWithoutReplacingTarget` |
| SNAP-F14 | 정상 | 캡처 시점·횟수와 파일 교체 | 기존 파일, capture에서 임시 파일·원본 관찰 | 준비 후 1회 capture, encode까지 원본 유지, 마지막에 교체 | `SnapshotFileStoreTest#capturesOnceAfterStagingBeforeEncoding` |
| SNAP-F15 | 예외 | 개방 전 실패에는 캡처하지 않음 | 디렉터리·부모 없는 대상 | IOException, capture 0회 | `SnapshotFileStoreTest#doesNotCaptureWhenPreparationFails` |
| SNAP-F16 | 예외 | 캡처 실패 정리 | 기존/새 경로, 예외 Supplier | 동일 예외, 원본 유지 또는 대상 없음, 임시 파일 없음 | `SnapshotFileStoreTest#captureFailurePreservesTargetAndCleansStage` |
| SNAP-F17 | 예외 | 부분 기록 실패 정리 | 일부 기록 후 IOException Codec | 동일 예외, 기존 바이트 유지, 임시 파일 없음 | `SnapshotFileStoreTest#encodingFailurePreservesTargetAndClosesOutput` |
| SNAP-F18 | 예외 | null 캡처 계약 | null Supplier 또는 null 반환 | NullPointerException, 원본·파일 목록 유지 | `SnapshotFileStoreTest#rejectsNullCaptureWithoutReplacingTarget` |
| SNAP-F19 | 정상·경계 | 설정해도 raw API는 순서를 바꾸지 않음 | 중복·범위 밖 좌표를 포함한 원시 목록 | read/write raw 왕복 동일 | `SnapshotFileStoreTest#keepsRawApiIndependentOfLayout` |

기존 `PatternDataTest#rejectsLossyCancerSnapshot`으로 클래식 전용 메서드의 암세포 거부를 유지한다. SAFE-10~13이 검증하는 이동 실패·원자적 이동 미지원·정리 실패 경로는 새 저장 API에서도 기존 write 경로를 재사용한다. 비정상 암 부분집합 거부는 새 스냅샷 API의 계약이며 raw/legacy 경로를 변경하지 않는다.

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
