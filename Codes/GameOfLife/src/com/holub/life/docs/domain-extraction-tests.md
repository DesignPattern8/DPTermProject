# 도메인 추출 특성 테스트 (#24)

- 최초 추출 기준: e38ce7a. 2026-11-04 main 병합 후 #16·#17·#18의 확정 계약을 반영한다(#29). 새 기능·버그 수정·새 패턴 도입 없이 메서드를 추출한다.
- 기존 CL-G/E/M/C 33개와 smoke 1개의 입력·기대값은 유지한다. 패키지 import와 추출된 메서드 호출만 조정한다.
- 구현 전 아래 케이스를 정의하고 원본으로 기대값을 확보한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| EXT-R01 | 정상 | 셀·블록 경계·활성 색 보존 | 64×64에 glider, 512px | 원본 픽셀 SHA-256 일치 | RenderingExtractionTest#fullRenderMatchesOriginal |
| EXT-R02 | 경계 | 빈 보드 렌더링 보존 | 빈 64×64, 512px | 원본 픽셀 SHA-256 일치 | RenderingExtractionTest#emptyRenderMatchesOriginal |
| EXT-R03 | 정상 | 부분 갱신·안정화 뒤 마지막 갱신 보존 | glider 4 tick, 동일 BufferedImage에 drawAll=false | 각 프레임 원본 SHA-256 일치 | RenderingExtractionTest#incrementalFramesMatchOriginal |
| EXT-R05 | 경계 | 안정화 직후 마지막 부분 갱신 유지 | block → tick → 부분 갱신 | 원본 픽셀 SHA-256 일치 | RenderingExtractionTest#stableBlockGetsItsLastRefresh |
| EXT-R04 | 경계 | 정수 나눗셈 좌표 변환 보존 | 폭 511px, 클릭 (63,63), (0,0), (510,510) | 원본 좌표·예외와 동일 | RenderingExtractionTest#unevenSurfaceKeepsOriginalClickBehavior |
| EXT-M01 | 예외 | 잘못된 파일 로드 시 기존 Clear 순서 보존 | 살아 있는 보드에 garbage | IOException 이후 빈 보드 | LoadFailureCharacterizationTest#invalidFileStillClearsBoard |
| EXT-M02 | 경계 | Point 아닌 원소 거부 | 직렬화된 List<String> | IOException, 빈 보드 | LoadFailureCharacterizationTest#nonPointElementsThrowIOException |
| EXT-M03 | 예외 | null 컬렉션을 파일 오류로 처리 | 직렬화된 null | IOException, 빈 보드 | LoadFailureCharacterizationTest#nullCollectionThrowsIOException |
| EXT-A01 | 정상 | 제어 추출 후 한 세대 진행 | blinker | 한 tick 뒤 기존 수직 좌표 | GameControllerTest#tickAdvancesOneGeneration |
| EXT-A02 | 예외 | 파일 열기 실패 시 Clear 이전에 실패 | 존재하지 않는 경로 | 기존 보드 유지 | GameControllerTest#missingFileKeepsBoard |
| EXT-A03 | 예외 | 파일 해석 실패 시 Clear 이후에 실패 | garbage 파일 | 기존 보드가 비워짐 | GameControllerTest#invalidFileClearsBoard |
| EXT-A04 | 경계 | 기존 타이머 주기·즉시 시작·취소 유지 | 수동 Timer 대역 | 지연 0, 요청 주기, 교체/정지 시 취소 | ClockTest#scheduleAndStopKeepOriginalTiming |
| EXT-U01 | 정상 | Go 메뉴 속도·Tick 매핑 유지 | 각 메뉴 명령 | 500/150/70/30/0ms 및 단일 tick | GameMenuTest#goCommandsKeepOriginalActions |
| EXT-U02 | 경계 | 메뉴 선택 중 tick 억제 유지 | 선택 경로 유/무 | 선택 중 미진행, 해제 후 진행 | GameMenuTest#openMenuSuppressesTick |

픽셀 기대값은 원본 src를 별도 임시 디렉터리에서 컴파일하여 생성한다. 테스트 실행 시 원본 복제본에 의존하지 않는다. 테스트별 도메인 문서에서 이 정의를 링크한다.

추가 제어 추출 케이스(구현 전 정의):

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| EXT-A05 | 정상 | 실제 파일 경로의 중복 없는 저장과 복원 유지 | 셀 하나 저장 → Clear → Load | 파일 좌표 1개, 복원 셀 1개 | GameControllerTest#storeWritesEachCoordinateOnceAndRoundTrip |
| EXT-A06 | 예외 | 출력 파일 열기 실패 시 기존 보드 유지 | 디렉터리를 저장 경로로 전달 | IOException, 기존 셀 유지 | GameControllerTest#invalidOutputDoesNotClearBoard |

| EXT-N01 | 정상/경계/예외 | 실제 Swing 연결의 전후 동작 보존 | 메뉴 10개, 클릭, Tick, Store, Clear, Load, 취소, 잘못된 파일, Exit | 원본/변경본 모두 같은 검증 통과 및 Exit 코드 0 | NativeUiSmoke#main |

EXT-N01은 디스플레이가 있는 환경에서 별도 프로세스로 실행한다. CI의 headless JUnit을 비활성화하거나 건너뛰게 하지 않는다.
