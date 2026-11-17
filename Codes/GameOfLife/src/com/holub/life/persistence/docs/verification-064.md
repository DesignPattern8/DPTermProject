# #64 안전한 Store 검증 기록

- 날짜: 2026-11-16
- 기준: `origin/refactor/base` 084d6c7 / 브랜치: `codex/c-safe-store-64`
- 환경: macOS arm64, JDK 26.0.2.1, Gradle wrapper 9.8.2, JUnit 5.13.4. Gradle 런처는 JAVA_HOME으로 지정한 JDK 26, 컴파일/실행 toolchain은 프로젝트 .gradle/jdks의 JDK 26이다.
- Docker를 사용하지 않았고 이 작업에서 만든 컨테이너는 없다.

## 테스트 먼저 작성한 증거

1. 변경 전 `./gradlew test --no-daemon`: **177개 통과**.
2. 004 설계·실패 계약과 `test-cases.md`의 SAFE-01~14를 먼저 정의했다.
3. production을 바꾸기 전에 `SafeBoardFileStoreTest`와 Controller 회귀 테스트를 작성하고 실행했다.
   - 명령: `./gradlew test --tests '*SafeBoardFileStoreTest' --tests '*GameControllerPersistenceTest' --no-daemon`
   - **12개 중 6개 실패**. 컴파일 실패가 아니라 바이트/파일 존재 assertion 실패다.
   - capture 실패: 원본 4바이트 → 0바이트.
   - 부분 기록 실패: 원본 4바이트 → 일부 기록 3바이트.
   - 새 경로 기록 실패: 대상 파일이 남음.
   - 기록 도중 원본 읽기: 원래 바이트 대신 일부 기록을 읽음.
   - Controller 실패: 원본 3바이트 → 1바이트.
   - 추가한 링크 거부 정책: 기존 코드는 링크를 따라가 capture를 실행함.
4. 이동 실패·원자적 이동 미지원·stream 종료·정리 실패 테스트를 작성한 뒤 BoardFileStore를 구현했다.
5. 수정 후 관련 20개 테스트 통과, 전체 **191개 통과(실패 0 / 오류 0 / skipped 0)**. 기존 177개 + 새 14개다. 기존 테스트를 삭제하거나 비활성화하지 않았다.

## 전체 검증

```sh
./gradlew test renderDiagrams -Pdir=src/com/holub/life/persistence --no-daemon
./gradlew renderDiagrams -Pdir=src/com/holub/life/persistence --no-daemon
```

첫 명령에서 테스트 191개가 통과했고 시퀀스 다이어그램의 줄바꿈 문법 오류로 렌더링만 실패했다. 해당 문법을 수정한 뒤 두 번째 명령으로 PNG/SVG 렌더링을 통과시켰다. 이후 그림 문구를 실제 실패 단계에 맞춰 다듬고 다시 렌더링했다. 새 두 그림을 시각적으로 확인하고, 동일 소스의 과거 그림이 재생성된 차이는 원래대로 보존했다.

수치는 `build/test-results/test/TEST-*.xml`의 tests/failures/errors/skipped 합산이다. HTML 보고서는 `build/reports/tests/test/index.html`에 있다. 전체 테스트에는 Load/Store·Clock·일반/암세포·메뉴·렌더링 기존 검증이 포함된다. 이번 변경에서 native UI 수동 조작을 추가로 수행하지 않았다.

## 실패·자원 검증의 범위

- 기존 파일/새 경로에서 실제 바이트와 디렉터리 목록을 비교하여 원본 보존 및 임시 파일 정리를 검증한다.
- 이동 함수 대역은 실제 임시 파일 경로를 받는다. 완성된 바이트, 동일 부모 폴더, 닫힌 stream을 확인한 뒤 실제 atomic move를 실행하는 테스트도 포함한다.
- 이동 실패와 미지원 파일 시스템은 대역 예외로 결정적으로 재현했다. 실제 다른 파일 시스템/네트워크 드라이브를 마운트해 시험한 것은 아니다. 비원자적 fallback이 없음을 이동 횟수와 원본 바이트로 검증한다.
- 삭제 실패는 테스트 소유 임시 경로를 비어 있지 않은 디렉터리로 바꿔 재현한다. 원래 예외와 suppressed 삭제 오류/잔여 경로를 검증하고 finally에서 해당 자원을 직접 정리한다.
- POSIX 권한과 링크 테스트는 지원 여부를 검사한다. 이번 macOS 실행에서는 모두 실행됐으며 건너뛴 테스트가 없다.
- Codec 오류, RuntimeException capture 오류, 준비 오류를 직접 주입했다. OS의 실제 close 오류·전원 중단을 재현하지는 않았다. close는 try-with-resources 안에 있고 실패 시 교체 전에 정리 경로로 전파한다.

## 범위와 제한

공개 시그니처/파일 형식/Load 정책은 유지했다. write의 실패 안전성과 대상 경로/권한 정책은 004에 명시적으로 변경했다. owner/ACL/확장 속성 보존, 강제 종료 뒤 잔여 파일 복구, 외부 프로세스와의 동시 변경, fsync 내구성은 보장하지 않는다. Windows 파일 시스템에서의 직접 실행 결과는 없다.
