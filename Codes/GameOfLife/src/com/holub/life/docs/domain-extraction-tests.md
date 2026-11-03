# 도메인 추출 특성 테스트 (#24)

- 기준: e38ce7a. 새 기능·버그 수정·새 패턴 도입 없이 메서드를 추출한다.
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
| EXT-M02 | 경계 | Point 아닌 원소를 즉시 검증하지 않음 | 직렬화된 List<String> | 로드 성공, 빈 보드 | LoadFailureCharacterizationTest#nonPointElementsRemainIgnored |
| EXT-M03 | 예외 | null 컬렉션의 기존 실패 유지 | 직렬화된 null | NullPointerException, 빈 보드 | LoadFailureCharacterizationTest#nullCollectionStillFailsDuringRestore |
| EXT-A01 | 정상 | 제어 추출 후 한 세대 진행 | blinker | 한 tick 뒤 기존 수직 좌표 | GameControllerTest#tickAdvancesOneGeneration |
| EXT-A02 | 예외 | 파일 열기 실패 시 Clear 이전에 실패 | 존재하지 않는 경로 | 기존 보드 유지 | GameControllerTest#missingFileKeepsBoard |
| EXT-A03 | 예외 | 파일 해석 실패 시 Clear 이후에 실패 | garbage 파일 | 기존 보드가 비워짐 | GameControllerTest#invalidFileClearsBoard |
| EXT-A04 | 경계 | 기존 타이머 주기·즉시 시작·취소 유지 | 수동 Timer 대역 | 지연 0, 요청 주기, 교체/정지 시 취소 | ClockTest#scheduleAndStopKeepOriginalTiming |
| EXT-U01 | 정상 | Go 메뉴 속도·Tick 매핑 유지 | 각 메뉴 명령 | 500/150/70/30/0ms 및 단일 tick | GameMenuTest#goCommandsKeepOriginalActions |
| EXT-U02 | 경계 | 메뉴 선택 중 tick 억제 유지 | 선택 경로 유/무 | 선택 중 미진행, 해제 후 진행 | GameMenuTest#openMenuSuppressesTick |

픽셀 기대값은 원본 src를 별도 임시 디렉터리에서 컴파일하여 생성한다. 테스트 실행 시 원본 복제본에 의존하지 않는다. 테스트별 도메인 문서에서 이 정의를 링크한다.
