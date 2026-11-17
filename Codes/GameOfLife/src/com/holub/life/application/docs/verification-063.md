# #63 안전한 Load 검증

- 날짜: 2026-11-17 / 기준: refactor/base `084d6c7810eb9400d6f2391da9945a91900f7ff4`
- 환경: macOS / Temurin 26.0.2.1+1 / Gradle 9.8.2 / JUnit 5.13.4 / headless
- [설계 결정·계약](005-safe-load.md), [구현 전 케이스](test-cases.md#63-안전한-load--구현-전-정의-2026-11-17)

## 테스트 우선 기록

| 단계 | 대상 | 결과 |
| --- | --- | --- |
| 기준선 | 변경 전 전체 JUnit | 177 성공 |
| RED | 새 GameControllerLoadTest 9개 + GameControllerTest 17개 + GameControllerPersistenceTest 2개 | 28개 중 9 실패(신규 6 + 기존 계약 갱신 3) |
| GREEN | 동일한 28개 | 모두 성공 |
| 회귀 | UI 오류 표시에서 보드 보존 검증을 보강한 최종 전체 실행 | 186 성공, 실패/오류/skipped 0 |
| 문서 | application만 PlantUML 렌더링 | 성공, 005 전후 PNG·SVG 확인 |

처음 경계 파일의 다음 tick까지 검증할 때는 10개가 실패했다. 그중 1개는 정상 Load 이후 블록 경계 출생 누락이라는 별도 기존 결함이었다. 변경 전 코드의 독립 실행으로 재현하여 [#81](https://github.com/DesignPattern8/DPTermProject/issues/81)에 기록하고, 이번 LOAD-09의 범위는 기존 좌표 배치 정책으로 한정했다. 이후 생산 코드를 수정하기 전에 #63의 9개 실패를 다시 확인했다.

실패는 빈 보드로 바뀜, 실행 중 예약 callback이 null로 바뀜, 정지 횟수 0 대신 1, 실패에도 EDITED 발생, Codec 해석 중 이미 기존 보드가 비워짐이라는 실제 assertion이었다. 컴파일 실패를 RED로 계산하지 않았다.

## 기존 테스트의 기대값 변경

#63은 별도 버그 수정이다. 다음 과거 동작 고정 테스트를 삭제/비활성화하지 않고 목표 계약으로 갱신했다.

| 이전 | 현재 검증 |
| --- | --- |
| invalidFileClearsBoard | invalidFileKeepsBoard — 원래 좌표 유지 |
| APP-R09 failedLoadStillNotifiesEdited | failedLoadDoesNotNotifyEdited — 알림 없음 |
| preservesOpenAndDecodeFailureOrder | preservesStateOnOpenAndDecodeFailure — Load 오류 시 Clock/보드 유지 |
| UI-R05 loadFailureShowsReadFailed | Read Failed 문구에 기존 배치 유지 검증 추가 |
| NativeUiSmoke invalid-load clear | invalid-load preservation — 실패 직전 좌표 집합과 동일 |

EXT-M01~03의 ClassicBoard 테스트 helper는 원본 clear-before-read 순서를 독립적으로 고정한 특성 테스트이므로 그대로 유지한다. 현재 GameController 동작은 LOAD/APP 테스트로 검증한다. 이전 문서의 특성 기록과 현재 계약을 구분했다.

## 재실행

JDK 26의 JAVA_HOME을 설정하고 Codes/GameOfLife에서 실행한다.

```sh
./gradlew test --tests '*GameControllerLoadTest' --tests '*GameControllerTest' --tests '*GameControllerPersistenceTest' --no-daemon
./gradlew test --no-daemon
./gradlew renderDiagrams -Pdir=src/com/holub/life/application --no-daemon
```

JUnit 합계는 `build/test-results/test/TEST-*.xml`의 tests/failures/errors/skipped 합계다. 새 Clock 테스트는 실제 타이머·sleep 대신 수동 TickSource를 사용한다. 실행 중 예약·속도 유지와 블록 경계 blinker의 다음 tick을 확인한다. 세포 상태 확인에는 활성 정보를 바꿀 수 있는 createMemento 대신 Board.snapshot과 셀의 isCancer를 사용한다.

실제 FileInputStream을 받는 Codec 대역으로 부분 읽기 실패 후 동일 IOException 전달과 stream 종료를 확인한다. 정상 읽기에서는 해석 중 기존 상태, 변경 알림 시 새 상태·정지 Clock·닫힌 입력을 확인한다. 기존 classic/cancer 저장 왕복, stream 정리, 보드 호출 직렬화, Clear·Go/Tick·마우스·오류 대화상자 테스트도 유지한다.

## 범위와 한계

- 전체 25파일: 생산 코드 1, 테스트 5, 문서 13, 전후 다이어그램 6. 생산 GameController는 +8/-15 LOC(공백 포함, 순 -7), 타입 추가/삭제 0. 별도 Clock 상태 API나 rollback 계층을 추가하지 않았다.
- 렌더링된 과거 003/004/before/after 출력은 기준 내용으로 복구했다. 신규 005 출력만 커밋한다. 다른 도메인 다이어그램·루트 원본 diagrams는 변경하지 않았다.
- NativeUiSmoke 소스는 새 계약으로 갱신하고 컴파일했으며, 네이티브 창을 띄우는 EXT-N01 실행은 하지 않았다. UI 변경 계약은 headless GameMenuTest로 검증했다.
- Docker·서버·별도 테스트 컨테이너를 실행하지 않았다. 새 테스트는 수동 TickSource와 JUnit TempDir만 사용하며 기존 Timer 테스트는 기존 정리 로직을 유지한다.
- 파일 개방/해석/닫기/변환 실패의 무변경 경계다. Cell.clear/transfer나 Clock/리스너의 프로그램 오류까지 원자적 복구하지 않는다. 실패 후 대기 중인 정상 tick은 진행할 수 있다.
- #64·#65는 아직 별도 PR이므로 이 브랜치에 포함하지 않았다. 두 변경 병합 뒤 제한된 입력 오류와 안전한 저장의 조합 검증이 필요하다. 정상 Load의 경계 세대 결함은 #81, 모델 계약 변경은 #71·#73에서 추적한다.
- CI 결과는 PR의 test check에서 별도로 확인한다.
