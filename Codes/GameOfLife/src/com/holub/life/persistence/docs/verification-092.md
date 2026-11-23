# #92 스냅샷 파일 API 검증

- 날짜: 2026-11-18
- 기준: `origin/refactor/base` `c2d09a939a62fc17235646caeab14f09739f9a84`
- 브랜치: `codex/c-snapshot-file-api`
- 환경: macOS arm64, JDK 26.0.2.1, Gradle wrapper 9.8.2, JUnit 5.13.4. 프로젝트 로컬 JDK를 JAVA_HOME으로 지정했다. 설정 파일·JDK는 커밋하지 않는다.
- Docker는 사용하지 않았으며 이 작업에서 시작한 컨테이너는 없다.

## 테스트 우선 순서

1. 변경 전 `./gradlew test --no-daemon`: 기존 **356개 통과**, 실패/오류/skipped 0.
2. `test-cases.md`의 SNAP-P01~05·SNAP-F01~19를 먼저 정의했다.
3. `PatternDataSnapshotTest`, `SnapshotFileStoreTest`를 작성했다. production 변경 전 실행은 **새 메서드·생성자 미존재로 compileTestJava 실패(29개 컴파일 오류)**였다. 동작 assertion 실패로 표현하지 않는다.
4. production 2파일을 변경하고 관련 테스트를 실행했다. **69개 통과**: 새 48개와 기존 21개. 기존 기대값을 변경하지 않았다.
5. 전체 테스트 및 C 다이어그램 렌더링: **404개 통과**, 실패/오류/skipped 0. 기존 356개 + 새 파라미터 포함 48개다.

```sh
# 구현 전 새 테스트 실행 (컴파일 실패)
./gradlew test --tests '*PatternDataSnapshotTest' --tests '*SnapshotFileStoreTest' --no-daemon

# 구현 후 관련 경로
./gradlew test --tests '*PatternDataSnapshotTest' --tests '*SnapshotFileStoreTest' \
  --tests '*PatternDataTest' --tests '*BoardFileStoreTest' --tests '*AtomicCommitTest' --no-daemon

# 전체 검증 및 C 범위 렌더링
./gradlew test renderDiagrams -Pdir=src/com/holub/life/persistence --no-daemon
```

개수는 `build/test-results/test/TEST-*.xml`의 tests/failures/errors/skipped 합계다. `*BoardFileStoreTest`에는 기존 SafeBoardFileStoreTest도 포함된다. 결과 HTML은 `build/reports/tests/test/index.html`이다. 테스트 로그는 로컬 `/tmp/dp-c-snapshot-{baseline,red,green,full}.log`에 보관했다.

## 검증한 동작

- **실제 파일 왕복:** 기존 일반·암 파일 기록 → 새 크기 지정 read → 실제 CompositeBoard.restore 후 스냅샷 비교. 기존 고정 암 fixture와 Codec 테스트도 전체 실행에 포함된다.
- **출력 순서:** 새 비교기를 테스트에 복제하지 않고 실제 `createMemento().liveCells()` 목록과 직렬화된 결과를 비교했다. 64×64의 [8,1], 같은 8×8 크기의 [4,1]과 [1], 12×12의 [6,2,1], 1×1의 [1]에서 모든 칸을 생존시켜 순서를 확인했다. 암세포가 섞인 여러 블록의 목록 순서와 종류도 확인했다.
- **좌표 경계:** 일반·암 좌표의 중복/음수/크기와 같은 값/int 극값을 필터링하고, 유효한 마지막 모서리는 유지한다. 빈·직사각형 값도 확인했다. 생존 집합 밖 암은 raw로 재읽으면 보존되지만 새 스냅샷 read에서는 원인을 포함한 IOException이다.
- **수명·실패:** 정상·해석/변환 실패에서 실제 입력 stream 종료, 캡처 1회·임시 파일 준비 뒤 실행, null/예외 캡처·부분 인코딩·크기 불일치에서 원본 바이트와 디렉터리 목록 보존을 확인했다. 실패 뒤 출력 stream도 닫힌다. 기존 원자적 이동 대역·미지원·정리 실패 테스트를 함께 유지했다.
- **기존 계약:** raw API의 순서·중복·범위 밖 값, 클래식 전용 변환의 암세포 거부, B의 `loadedBoundaryPatternKeepsLegacyCoordinatePolicy`와 `preservesTraversalAndCancer`를 포함한 전체 회귀 테스트를 유지했다.

## 문서·포맷 검증

- PlantUML 1.2026.9로 C 도메인만 지정하여 렌더링했다. 새 전후 PNG를 열어 클래스·관계·설명 표시를 확인했다. 이전 그림과 다른 도메인 그림의 diff는 없다.
- 수정/추가 Java 4파일에 google-java-format을 적용하고 `--dry-run --set-exit-if-changed`가 성공했다.
- `git diff --check` 통과. production LOC는 +99/-10, 새 production 타입은 0개다. JUnit 테스트 클래스 2개는 별도다.

## 범위와 한계

이 PR은 C API 제공까지다. 앱 Controller는 기존 경로를 유지하며 새 경로의 UI 통합·Cell/Mapper 제거는 B #88에서 검증해야 한다. 모델 스냅샷에 없는 2인용 소유자, 불규칙 트리, 잘못된 블록 설정이 실제 모델과 다른 경우의 자동 탐지는 지원하지 않는다. 네이티브 UI 수동 조작·Windows 실행·실제 전원 중단·OS close 오류 주입은 수행하지 않았다. 정렬 성능은 복잡도만 분석했고 시간·메모리 벤치마크 수치는 주장하지 않는다.
