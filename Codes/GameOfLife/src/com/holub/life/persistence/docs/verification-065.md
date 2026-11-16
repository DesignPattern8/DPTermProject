# #65 Legacy 입력 정책 검증

- 날짜: 2026-11-16 / 기준: `refactor/base` `084d6c7810eb9400d6f2391da9945a91900f7ff4`
- 환경: macOS, Temurin 26.0.2.1+1, Gradle wrapper 9.8.2, JUnit 5.13.4, `java.awt.headless=true`
- 설계/정책: [005](005-legacy-input-policy.md), [테스트 정의](test-cases.md#65-역직렬화-정책--구현-전-정의-2026-11-16)

## 테스트 우선 실행 기록

| 단계 | 실행 범위 | 결과 |
| --- | --- | --- |
| 기준선 | 기존 전체 테스트 | 177 성공 |
| RED | 테스트 정의 후 새 LegacyInputPolicyTest 작성, 기존 생산 코드로 실행 | 15개 중 10 실패, 5 성공 |
| GREEN | 필터/바이트 한도/원소 한도 구현 후 새 정책 + LegacyLifeCodecTest + CancerCodecTest + ClassicMementoTest | 37 성공 |
| 회귀 | 전체 JUnit | 192 성공, 실패/오류/skip 0 |
| 문서 | C 도메인 PlantUML 렌더링 | 성공, 신규 전/후 PNG·SVG 확인 |

RED에서 금지 타입의 readObject 표식이 실행되고, Point 하위 클래스/Vector/초과 원소가 허용되며, 바이트/skip/배열/깊이/참조 한도가 적용되지 않는 것을 확인했다. 기존 코드가 통과하던 5개 호환·소유권·전역 필터 테스트는 구현 후에도 유지했다. 기존 테스트는 삭제·비활성화하지 않았다.

`Codes/GameOfLife`에서 JDK 26의 JAVA_HOME을 설정하고 실행한다.

```sh
./gradlew test --tests '*LegacyInputPolicyTest' --no-daemon
./gradlew test --tests '*LegacyInputPolicyTest' --tests '*LegacyLifeCodecTest' --tests '*CancerCodecTest' --tests '*ClassicMementoTest' --no-daemon
./gradlew test --no-daemon
./gradlew renderDiagrams -Pdir=src/com/holub/life/persistence --no-daemon
```

숫자는 `build/test-results/test/TEST-*.xml`의 tests/failures/errors/skipped 합계다. 단위/통합 테스트는 기존 Clear, Load, Store, 속도/Tick, 마우스 토글 등 기존 회귀를 포함한다. 실제 네이티브 창을 직접 조작한 수동 검증은 하지 않았다. CI는 PR의 test check에서 별도로 확인한다.

## Fixture와 한도 근거 재현

저장소 전체 이력에서 .life 파일을 찾지 못했으므로 초기 `e38ce7a`의 실제 writer를 별도 임시 디렉터리에서 컴파일했다. 아래 코드를 해당 원본 클래스 경로로 실행하여 생성한 파일을 테스트에 Base64로 고정했다. 외부 사용자 파일을 수집했다는 의미는 아니다. 정상/잘못된 암세포 fixture는 기존 #60/4435a31 기반 테스트를 그대로 재사용했다.

```java
Class<?> state = Class.forName("com.holub.life.Neighborhood$NeighborhoodState");
var ctor = state.getDeclaredConstructor();
ctor.setAccessible(true);
Object value = ctor.newInstance();
var mark = state.getMethod("markAsAlive", java.awt.Point.class);
mark.setAccessible(true);
mark.invoke(value, new java.awt.Point(6, 5));
mark.invoke(value, new java.awt.Point(-1, 1000));
mark.invoke(value, new java.awt.Point(6, 5));
var flush = state.getMethod("flush", java.io.OutputStream.class);
flush.setAccessible(true);
try (var out = new java.io.FileOutputStream("original-e38ce7a.life")) {
    flush.invoke(value, out);
}
```

원본 파일 SHA-256은 `0726876c6308d7293fcd162b8bbb1f8881124252d8f374dbbb2367b26beaa7b7`이다. 한도 결정에 쓴 구조 계수는 같은 JDK에서 아래 코드를 현행 main 클래스 경로로 실행해 측정했다. 모든 import는 JDK java.io/java.util 및 model/persistence 패키지다. 시간·힙 벤치마크가 아니다.

```java
for (int n : new int[] {4096, 16384}) {
    var cells = new ArrayList<CellPosition>();
    for (int i = 0; i < n; i++) cells.add(new CellPosition(i / 64, i % 64));
    var out = new ByteArrayOutputStream();
    new LegacyLifeCodec().write(new PatternData(cells, new HashSet<>(cells)), out);
    long[] max = new long[3];
    try (var in = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
        in.setObjectInputFilter(info -> {
            max[0] = Math.max(max[0], info.depth());
            max[1] = Math.max(max[1], info.references());
            max[2] = Math.max(max[2], info.arrayLength());
            return ObjectInputFilter.Status.UNDECIDED;
        });
        in.readObject();
    }
    System.out.println(n + " bytes=" + out.size() + " limits=" + Arrays.toString(max));
}
```

결과: `4096 bytes=114962 limits=[3, 8199, 8192]`, `16384 bytes=459026 limits=[3, 32775, 32768]`. 후자는 신규 왕복 테스트로도 검증한다. 참조 경계 테스트는 동일 Point를 반복하는 LinkedList의 JDK 26 계수(항목 수 +3)를 사용한다.

## 변경 범위와 검증 한계

- 전체 15파일: 생산 코드 1, 신규 테스트 1, 문서 7, 신규 전/후 다이어그램 원본·PNG·SVG 6. 생산 코드 diff는 +115/-2 LOC(공백 포함), 최상위 타입 추가 0, private stream 타입 추가 1이다.
- 렌더링 과정에서 다시 만들어진 기존 003/c-current/c-proposed 출력은 복구하여 이 PR에 포함하지 않는다. 루트 원본 diagrams는 수정하지 않았다.
- 전역 필터 테스트만 별도 JVM으로 격리하고 종료/실패 시 프로세스를 정리한다. Docker 및 별도 서버는 실행하지 않았다.
- 필터는 String 자체를 거부하지 않는다. 바이트 한도로 읽기/skip을 제한하고 복원 후 형식 검사를 유지한다. 전체 파일 크기·모든 CPU/힙 비용의 상한을 보장하지 않는다.
- 공개 시그니처·직렬화 형식은 유지한다. 허용 Collection 범위와 자원/쓰기 원소 한도는 행동 계약 변경이다. 외부 사용자 fixture 및 JDK 변경 시 추가 호환성 검증이 필요하다.
- #64의 안전한 Store는 이 브랜치에 포함하지 않았다. Codec의 출력 전 원소 검증이 기존 BoardFileStore의 선행 truncate를 되돌리지는 않는다. Load 실패 후 보드·Clock 보존은 #63 범위다.
