# 실행 테스트

구현 전 정의: [EXT-A01~06](../../docs/domain-extraction-tests.md).

GameControllerTest는 정상 세대, 실제 파일 왕복과 좌표당 1회 기록, 파일 열기/해석/출력 실패 경로를 확인한다. ClockTest는 실제 시간을 기다리지 않는 Timer 대역으로 즉시 시작(0), 주기, 교체·정지 시 취소, 수동 tick을 확인한다. 테스트가 소유한 Timer는 finally/AfterEach에서 cancel한다.

## 불러오기·저장 후 파일 스트림 해제 (#41)

`GameController.load/store`가 성공·실패와 관계없이 연 파일 스트림을 닫는지 확인한다. 기존 EXT-A02/A03/A05/A06의 보드 상태 기대값은 바꾸지 않는다.

스트림이 열려 있는지는 두 가지로 판정한다.

- 파일 삭제: Windows는 열린 파일을 지울 수 없으므로 삭제가 실패한다.
- 열린 파일 디스크립터 수: Linux·macOS(CI 포함)는 열린 파일도 지워지므로, `UnixOperatingSystemMXBean.getOpenFileDescriptorCount()`가 호출 전보다 늘지 않았는지 본다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| APP-S01 | 정상 | 정상 Load 후 입력 스트림 해제 | 셀 하나 저장한 파일 → Load | 셀 복원, 파일 삭제 가능·열린 파일 수 그대로 | GameControllerTest#successfulLoadReleasesFile |
| APP-S02 | 예외 | 형식이 잘못된 파일 Load 실패 후 입력 스트림 해제 | garbage 4바이트 파일 | IOException, 파일 삭제 가능·열린 파일 수 그대로 | GameControllerTest#invalidFileLoadReleasesFile |
| APP-S03 | 경계 | 빈 파일(0바이트) Load 실패 후 입력 스트림 해제 | 0바이트 파일 | IOException, 파일 삭제 가능·열린 파일 수 그대로 | GameControllerTest#emptyFileLoadReleasesFile |
| APP-S04 | 정상 | 정상 Store 후 출력 스트림 해제 | 셀 하나 → Store | 파일 삭제 가능·열린 파일 수 그대로 | GameControllerTest#successfulStoreReleasesFile |

한계: 출력 파일을 연 뒤 직렬화(`LegacyLifeCodec.flush`)가 실패하는 경우는 `GameController`가 스트림을 직접 열어 대역을 넣을 수 없어 테스트로 재현하지 못했다. `store`는 `load`와 같은 `try-with-resources` 구조로 고쳤다.
