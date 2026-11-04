# 도메인 추출 검증 (#24)

- 일자: 2026-11-03
- 원본: e38ce7a
- 환경: macOS arm64, 저장소 로컬 OpenJDK 26.0.2.1, Gradle 9.8.2, JUnit 5.13.4.
- 기존 JDK/Gradle 설정을 사용했다. Docker나 새 테스트 의존성은 추가하지 않았다.

## JUnit
`Codes/GameOfLife`에서 실행:

```sh
./gradlew test --no-daemon --rerun-tasks
```

| 시점 | 결과 |
| --- | --- |
| 원본 | 기존 34개 통과 |
| 원본 + 추출 전 추가 특성 테스트 | 42개 통과 |
| 파일 인코딩 추출 후 | 42개 통과 |
| 모델·렌더링 추출 후 | 42개 통과 |
| 제어·메뉴·파일 경로 추출 후 | 50개 통과, 실패 0, skipped 0 |

기존 ClassicCell/Edit/Generation/Memento/Smoke 테스트의 입력·검증식은 유지했다. 패키지 import와 liveResident의 추출된 클릭 함수 호출만 변경했다. ClassicBoard는 원본 메뉴 경로를 재현하는 도우미로서 새 위치의 Codec/좌표 함수를 호출한다. ReferenceLife는 변경하지 않았다.

## 픽셀·파일 호환
RenderingExtractionTest의 기대값은 원본 소스를 임시 디렉터리에 별도 컴파일해 생성했다. 512×512 ARGB 픽셀을 행 순서로 A/R/G/B 바이트로 나열해 SHA-256을 계산한다. 원본과 변경본의 빈 보드·glider·4세대 부분 갱신·block 안정화 프레임이 일치한다. 전체 화면 글꼴/플랫폼 차이가 섞이지 않도록 보드만 BufferedImage로 그렸다.

실제 메뉴로 저장한 3개 셀의 파일을 원본/변경본에서 byte 비교했다. 모두 6개 Point를 기록하며 SHA-256은 다음과 같다.

```text
00d2ba464c99fae289948132ce4d58165bef2b5f2adc800236aae529b04c087c
```

## 실제 Swing 흐름
NativeUiSmoke는 별도 JVM에서 실제 JFrame/JMenuItem/JFileChooser/JOptionPane을 사용한다. 원본과 변경본에서 각각 다음 시나리오를 통과하고 Exit 메뉴로 종료 코드 0을 반환했다.

- 창 표시, 512×512 보드, 기존 Grid/Go 메뉴 10개
- MouseEvent로 blinker 배치 → Tick 메뉴 → 다음 세대
- Store → 파일 중복 좌표 확인 → Clear → Load
- Load 취소 → 기존 Read Failed! 대화상자 → 기존 보드 유지
- 잘못된 파일 Load → 기존 Read Failed! 대화상자 → 기존 보드가 비워짐
- Exit 메뉴 → JVM 종료

재현(디스플레이가 있는 환경, 저장 파일은 임시 디렉터리):

```sh
./gradlew testClasses --no-daemon
smoke_dir=$(mktemp -d)
java -ea -Djava.awt.headless=false \
  -cp build/classes/java/main:build/classes/java/test \
  com.holub.life.ui.NativeUiSmoke "$smoke_dir"
```

이 실행은 앱 창을 잠시 표시하고 Exit로 종료한다. CI의 50개 headless JUnit 테스트와 별개다. 실제 메뉴의 시간 경과를 기다리는 속도 테스트 대신 ClockTest/GameMenuTest가 수동 Timer 대역으로 500/150/70/30ms, 즉시 시작, 교체/정지 취소, 선택 중 tick 억제를 검증한다. 대역 Timer는 실행을 다음 날로 예약해 TimerTask의 상태를 보존하고 finally에서 취소한다.

## 범위와 한계
원본의 알려진 잘못된 동작을 고정했으며 개선하지 않았다. 기존 동시성 보장도 확대하지 않았다. 성능을 측정하지 않아 속도 개선은 주장하지 않는다. API의 패키지/메서드 위치는 바뀌었으므로 저장소 외부 호출자의 소스 호환성은 보장하지 않는다.

## 2026-11-04 main 병합 후 CI 복구 (#29)

관련: [이슈 #29](https://github.com/DesignPattern8/DPTermProject/issues/29), [실패 실행](https://github.com/DesignPattern8/DPTermProject/actions/runs/41780756517).
위 2026-11-03 결과는 최초 추출 시점의 기록이다. 현재 저장·오류 계약은 main의 #16·#17을 따른다.

- 병합 커밋 `1d7cf76`에서 테스트 컴파일 오류 4건을 로컬에서도 재현했다. `ClassicMementoTest`가 삭제된 `NeighborhoodState.load`를 호출하고, `ClassicCellTest`가 이동한 모델 패키지의 내부 계측 필드에 접근했다.
- 직접 load 호출을 `LegacyLifeCodec.load`로 이전하고, CL-C-08을 모델 패키지의 `NeighborhoodRecomputationTest`로 옮겼다. 기존 입력, tick 범위, 검증식은 유지했다.
- 공개 카운터 추가(A) 대신 동일 패키지 테스트 이동(B)을 선택했다. A는 기존 클래스 1개 필드의 접근 범위를 확대한다. B는 테스트 클래스 1개를 추가하고 테스트 메서드 1개를 옮기며 운영 API 변화와 운영 호출 간접성 증가는 0이다. 두 안 모두 Swing 창 없이 실행 가능하며 계측 비용은 동일하다. B는 모델 내부 계측 테스트를 같은 패키지에 추가할 수 있다.
- 컴파일 복구 및 main 계약으로 테스트 정렬 후 55개 중 6개 실패를 확인했다. 새 Codec에 #17의 Collection/Point 검증이 빠졌고, GameController와 ClassicBoard에는 #16에서 제거한 추가 STORE transfer가 남아 있었다.
- main에서 이미 확정된 검증을 Codec에 반영하고 중복 transfer를 제거했다. 과거 버그를 기대하던 추출 테스트와 NativeUiSmoke의 저장 개수도 해당 계약으로 갱신했다. 테스트 삭제·비활성화는 없다.
- `./gradlew clean test build --no-daemon`: **55개 통과, 실패 0, 오류 0, skipped 0**, BUILD SUCCESSFUL. 환경은 macOS arm64 / 저장소 로컬 JDK 26.0.2.1 / Gradle 9.8.2이다.
- 렌더링 픽셀, 세대 진행, 기존 중복 파일 호환, 메뉴·Clock headless 테스트도 통과했다. 이번에는 실제 Swing 창 smoke를 실행하지 않았으며 GitHub Actions의 재실행 결과는 아직 없다.
- 클래스 관계와 공개 시그니처는 그대로여서 기존 클래스 다이어그램은 변경하지 않았다. Docker는 사용하지 않았다. 후속 작업은 수정사항 push 후 원격 CI 확인이다.
