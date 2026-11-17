# C 파일·프리셋 공개 인터페이스

- 제공: C(도현) / 사용: B 실행 제어, 조립 지점 Life. 모델 값 타입은 A 제공.
- 변경 이력: 2026-11-16, #64 — write의 기존 파일 직접 truncate를 임시 파일 기록·원자적 교체로 변경. [004](004-atomic-file-replacement.md). 공개 시그니처는 유지한다.
- 변경 이력: 2026-11-14, #61 — 구 정적 load/flush/open API를 인스턴스 파일 API로 이전. 구현 결정은 [003](003-file-api-and-model-values.md). 팀원 합의 완료를 주장하지 않으며 변경 계약은 #61과 PR에서 검토한다.

- 변경 이력: 2026-11-16, #65 — 공개 시그니처 유지, Legacy 입력 허용 클래스와 자원 한도 축소, 쓰기 원소 한도 추가. [005](005-legacy-input-policy.md)와 #65 PR에서 검토하며 별도 팀원 합의 완료를 주장하지 않는다.

## 시그니처

```java
public record PatternData(List<CellPosition> cells, Set<CellPosition> cancerCells) {
    public BoardSnapshot toClassicSnapshot(int rows, int columns);
}
public interface PatternCodec {
    PatternData read(InputStream in) throws IOException;
    void write(PatternData data, OutputStream out) throws IOException;
}
public final class LegacyLifeCodec implements PatternCodec { /* public 기본 생성자 */ }
public final class BoardFileStore {
    public BoardFileStore(PatternCodec codec);
    public PatternData read(Path path) throws IOException;
    public PatternData read(Path path, Runnable afterOpen) throws IOException;
    public void write(Path path, PatternData data) throws IOException;
    public void write(Path path, Supplier<PatternData> capture) throws IOException;
}
public record PatternPreset(String id, String displayName, PatternData data) {}
public final class PresetCatalog {
    public List<PatternPreset> list();
    public Optional<PatternPreset> find(String id);
}
```

## 계약

- `PatternData`는 목록/집합과 원소의 null을 거부하고 방어 복사한다. A의 불변 `CellPosition`을 공유한다. 파일 데이터는 보드 크기를 모르므로 음수·범위 밖 좌표와 목록 순서·중복을 보존한다. 암세포 집합을 임의로 생존 목록과 합치거나 걸러내지 않는다(기존 해석 정책).
- `toClassicSnapshot`은 대상 크기를 받으며 유효 범위의 생존 좌표를 집합으로 만든다. 크기가 양수가 아니면 `IllegalArgumentException`, 암 정보가 있으면 `IllegalStateException`이다. A의 `Board.restore`는 같은 크기의 snapshot만 받는다.
- Codec은 입력 최상위 타입과 Point 원소를 검증한다. 잘못된 파일은 `IOException`; 파일/stream 오용의 모든 런타임 예외를 IOException으로 감싸지 않는다. stream은 호출자 소유이며 성공/실패 모두 Codec이 닫지 않는다. 쓰기 후 flush한다.
- Legacy 입력은 정확한 `Point` 및 기존 암세포 envelope와 [005의 명시적 컬렉션·JDK 보조 타입](005-legacy-input-policy.md#호환-근거와-허용-범위)만 허용한다. 임의 하위 클래스와 미지원 컬렉션은 객체 복원 전에 거부한다. 필터는 기존 JVM 제한과 병합한다.
- 첫 객체를 해석하는 동안 소비하는 바이트 ≤1 MiB, 깊이 ≤8, 참조 ≤65,536, 개별 배열 길이 ≤32,768, 생존/암세포 목록은 각각 ≤16,384개다. 금지 타입/그래프 초과는 `InvalidClassException`, 바이트/원소 초과는 `IOException`이다. 첫 객체 뒤의 데이터는 기존처럼 해석하지 않는다.
- Legacy 쓰기는 각 목록의 원소 수를 출력 시작 전에 검사한다. 초과하면 `IOException`이고 전달된 stream을 변경하지 않는다. 다만 현재 `BoardFileStore`의 선행 truncate는 별도 #64 변경 대상이다. 직렬화 바이트 형식과 좌표 해석은 유지한다.
- 일반 파일은 `LinkedList<Point>`를 기록한다. 암세포 파일은 기존 `LegacyLifeCodec$CancerStateData`, UID=1, Collection/Set 필드 형식을 유지한다. Point x=column/y=row다.
- Store는 자신이 연 stream을 try-with-resources로 닫는다. read 콜백은 입력 파일 개방 후, write 캡처는 같은 폴더의 임시 파일 준비·개방 후 한 번 실행한다. write는 기록·close·권한 적용 성공 후 원자적으로 대상 파일을 교체한다. 기존 파일을 먼저 truncate하지 않는다. 공개 write 시그니처와 B의 호출 방식은 유지한다.
- write는 일반 파일 또는 없는 대상만 지원한다. 최종 심볼릭 링크·디렉터리·특수 파일·쓰기 불가 대상은 capture 전에 IOException으로 거부한다. 부모 경로는 실제 경로로 해석하고 자동 생성하지 않는다. POSIX 기존 rwx 권한은 복사하고 새 파일은 임시 파일의 제한적인 기본 권한을 유지한다. owner/ACL/확장 속성·하드 링크 관계 보존은 보장하지 않는다.
- 캡처·기록·close·권한·이동 실패는 원래 예외를 전파하고 임시 파일 삭제를 시도한다. 삭제 실패는 잔여 경로를 담은 suppressed 예외로 제공한다. 원자적 이동/교체가 불가능하면 실패하며 비원자적 fallback은 없다. 강제 종료·전원 중단에 대한 내구성과 외부 프로세스의 동시 변경은 범위 밖이다. 상세 정책은 004를 따른다.
- Load 준비에서 B가 Clock 정지·Clear한다. 파일 없음은 기존 보드/Clock 유지, 내용 오류는 빈 보드/정지, 성공·실패 모두 EDITED 알림이다(#63에서 정책 변경 예정). read가 반환할 때 입력 stream은 이미 닫혔다. 이후 B가 transfer(LOAD)로 적용한다.
- B는 Load 전체 및 Store의 Memento 캡처·값 변환을 기존 lock으로 직렬화한다. 인코딩 중에는 불변 데이터만 사용하므로 모델 lock이 필요 없다. C 자체는 보드 스레드를 조정하지 않는다. Codec 대역의 동시 사용 안전성은 주입자가 보장한다.
- 카탈로그 순서는 block, blinker, glider이며 표시명은 Block, Blinker, Glider다. 좌표는 좌상단 기준 상대 위치다. 목록·패턴 데이터는 불변이며 알 수 없는 id(null 포함)는 Optional.empty다. `PatternPreset` 필드는 null을 거부한다. 화면 배치 원점·겹침·범위 처리와 없는 id에서 보드 무변경은 B/D #62의 계약이다.

## B 연동 API

```java
public GameController(Board board, Cell fileCells, Clock clock,
                      CancerGame cancerGame, BoardFileStore files);
public static PatternData PersistenceMapper.capture(NeighborhoodState state);
public static NeighborhoodState PersistenceMapper.restore(PatternData data);
```

기존 Controller 3/4인자 생성자는 유지하고 기본 legacy Store에 위임한다. `fileCells`와 `board`는 같은 보드여야 한다. Memento는 타입이 올바른 모델 소유 데이터라는 전제다. Mapper는 Point를 양방향 복사하며 캡처/복원 과정에서 보드를 순회하지 않는다. Store 캡처는 기존 `createMemento()` 한 번만 호출한다. D의 load/store(File) 시그니처는 유지한다.
