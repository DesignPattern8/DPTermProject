# #61 검증 기록

- 날짜: 2026-11-14
- 기준: origin/main `4435a31`, 작업 브랜치 `codex/c-domain-61`
- 환경: macOS arm64, Temurin JDK 26.0.2.1+1, Gradle wrapper 9.8.2, JUnit 5.13.4. 호스트 기본 JDK는 25여서 JDK 26을 임시 경로에 내려받아 JAVA_HOME으로 지정했다. Docker 컨테이너는 생성하지 않았다.

## 테스트 순서와 결과

1. `test-cases.md`에 C-01~13과 003의 API·실패 정책을 정의했다.
2. 새 API 테스트를 먼저 작성하고 기본 `./gradlew test --no-daemon`을 실행했다. PatternData/PatternCodec/생성자 등이 미구현이라 `compileTestJava`에서 실패했다. **동작 assertion 실패가 아니라 API 부재의 컴파일 실패**다.
3. 기존 코드에서 추가 특성 테스트 `GameControllerPersistenceTest` 2개와 기존 전체 테스트 **146개 통과**. 새 API 테스트는 임시 Gradle init의 test source 제외로 분리했다. 저장 순서·암세포 왕복·개방/해석 실패 시 Clock/보드/알림 동작을 고정했다.
4. 파일 API 구현 및 모든 정적 호출 이전 후 파일 관련 테스트와 기존 전체 테스트 통과. 프리셋 테스트는 기능 구현 전까지 임시 실행 설정에서 분리했다. 저장소 빌드 설정이나 기존 테스트를 삭제·비활성화하지 않았다.
5. 프리셋 구현 후 기본 설정으로 `./gradlew test renderDiagrams -Pdir=src/com/holub/life/persistence --no-daemon` 실행: **164개, 실패 0, 오류 0, skipped 0**. 기존 144개 + 새 20개다.

로컬 XML은 `Codes/GameOfLife/build/test-results/test/TEST-*.xml`, HTML 보고서는 `build/reports/tests/test/index.html`이다. 수치는 XML tests/failures/errors/skipped 합산이다. 기존 Clear/Load/Store/Go/Tick·편집·암세포·렌더링 테스트가 포함된다. native 창/Exit 수동 실행은 이번 작업에서 수행하지 않았으며 `NativeUiSmoke`는 새 모델 조회 경로로 이전해 컴파일했다.

## 파일 호환과 자원 수명

- 구현 전 Codec(4435a31)을 별도 클래스 경로로 컴파일하고 정상/잘못된 암세포 직렬화 fixture를 생성했다. 외부 사용자 파일은 아니다.
- `LegacyLifeCodecTest#readsPreRefactoringCancerFixture`가 고정 Base64 fixture를 읽는다. `CancerCodecTest#cancerFileWithNonPointEntryIsRejected`는 이전의 잘못된 원소 fixture를 읽어 동일 IOException 기대를 유지한다. 새 typed API가 잘못된 모델 원소를 만들 수 없으므로 fixture 생성만 대체했다.
- 새 Codec으로 정상 fixture를 다시 저장하고 **분리된 이전 Codec으로 읽어** 생존 좌표 순서와 암 좌표가 같은지 추가 확인했다. 중첩 직렬화 클래스 이름·필드 타입·UID 호환 검증이다.
- Store 테스트는 대역 Codec에 전달된 **실제 파일 스트림**을 성공/실패 후 다시 읽고/써서 IOException으로 닫힘을 확인한다. 준비 콜백 실패는 반복 호출 전후 Unix descriptor 수와 파일 삭제로 확인한다. Codec 비소유 계약은 close 기록 stream으로 확인한다.

## 문서 및 제한

- C 도메인만 지정해 PlantUML 1.2026.9로 PNG·SVG 생성 후 전/후 그림을 확인했다. 과거 c-current/c-proposed 렌더링 결과는 원래 버전을 보존한다.
- 구 Java 정적 API `load/flush/openForLoad/openForStore` 호출이 src/test에 남지 않음을 검색했다.
- 002는 역사 기록으로 남기고 상태를 003으로 대체했다. 공개 인터페이스 및 A/B README 링크를 갱신했다.
- 테스트는 현재 호환 계약을 검증하며 외부 모든 legacy 파일, 파일 교체 원자성, 역직렬화 제한, native UI 전체를 검증했다고 주장하지 않는다. 후속 #62~65의 범위는 003에 기록한다.
