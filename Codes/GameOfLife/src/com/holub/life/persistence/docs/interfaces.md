# C 파일·프리셋 공개 인터페이스

- 제공: C(도현) / 사용: B 실행 제어, 조립 지점 Life. 모델 값 타입은 A 제공.
- 변경 이력: 2026-11-14, #61 — 구 정적 load/flush/open API를 인스턴스 파일 API로 이전. 구현 결정은 [003](003-file-api-and-model-values.md). 팀원 합의 완료를 주장하지 않으며 변경 계약은 #61과 PR에서 검토한다.

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
- 일반 파일은 `LinkedList<Point>`를 기록한다. 암세포 파일은 기존 `LegacyLifeCodec$CancerStateData`, UID=1, Collection/Set 필드 형식을 유지한다. Point x=column/y=row다.
- Store는 자신이 연 파일 stream을 try-with-resources로 항상 닫는다. read 준비 콜백과 write 캡처 콜백은 파일 개방 성공 후 한 번 실행한다. 콜백/Codec 예외를 그대로 전파한다. 출력은 먼저 truncate하며 원자적 교체를 제공하지 않는다(#64).
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
