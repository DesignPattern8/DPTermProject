# C 도메인이 현재 공개하는 인터페이스

- 제공: C(도현), 저장·패턴 데이터 / 운영 사용: B(application) / 간접 사용: D(ui).
- 기준: `19bd091`, 2026-11-04. 분석 이슈 [#31](https://github.com/DesignPattern8/DPTermProject/issues/31).
- 변경 이력: #24에서 파일 처리를 추출했고, #29에서 main의 #16·#17 계약을 반영했다. 이 문서는 현재 계약의 명세이며 구현 변경은 없다.
- 여기서 인터페이스는 **다른 도메인이 호출하는 공개 계약**이다. 현재 C에는 Java `interface` 선언이 없고, 두 `public final` 클래스의 정적 메서드 네 개가 있다.

## 공개 시그니처와 직접 호출자

```java
// BoardFileStore
public static InputStream openForLoad(File file) throws FileNotFoundException;
public static OutputStream openForStore(File file) throws FileNotFoundException;

// LegacyLifeCodec
public static void load(NeighborhoodState state, InputStream in) throws IOException;
public static void flush(NeighborhoodState state, OutputStream out) throws IOException;
```

| API | 구현 근거 | 운영 호출자 | 테스트 호출자 |
| --- | --- | --- | --- |
| openForLoad | [BoardFileStore.java:9](../BoardFileStore.java#L9) | [B GameController.load:44](../../application/GameController.java#L44) | GameControllerTest에서 B를 통해 간접 호출 |
| openForStore | [BoardFileStore.java:13](../BoardFileStore.java#L13) | [B GameController.store:54](../../application/GameController.java#L54) | GameControllerTest에서 B를 통해 간접 호출 |
| load | [LegacyLifeCodec.java:12](../LegacyLifeCodec.java#L12) | [B GameController.load:48](../../application/GameController.java#L48) | ClassicBoard.load, ClassicMementoTest.loadDirectly |
| flush | [LegacyLifeCodec.java:27](../LegacyLifeCodec.java#L27) | [B GameController.store:57](../../application/GameController.java#L57) | ClassicBoard.store/liveCells, NativeUiSmoke.cells의 리플렉션 호출 |

테스트 파일은 `Codes/GameOfLife/test/com/holub/life/` 아래에 있다. 테스트의 직접 호출을 제품 도메인 의존 관계로 세지 않는다. [D GameMenu:55](../../ui/GameMenu.java#L55)는 FileDialogs로 경로를 고른 뒤 B의 load/store를 호출하고 IOException을 대화상자로 표시한다. A 모델은 C를 호출하지 않는다. 현재 의존 방향은 **D → B → C → A의 NeighborhoodState**다.

## openForLoad / openForStore 계약

- 입력은 실제 파일 경로를 나타내는 non-null File이다. 파일 선택 대화상자는 C의 책임이 아니다. `.life` 확장자는 D의 선택 필터이며 C가 확장자로 내용 형식을 보증하지 않는다.
- openForLoad는 FileInputStream을 연다. 경로가 없거나 읽을 수 없으면 FileNotFoundException이 호출자에게 전달된다.
- openForStore는 FileOutputStream을 연다. 기존 파일은 열 때 길이가 0으로 잘릴 수 있다. 부모 디렉터리 생성, 임시 파일 교체, 복구는 제공하지 않는다.
- 성공 시 **열린 스트림**을 반환하며, 닫는 책임은 호출자 B에게 있다. 파일 열기 이후 실패했을 때의 정리는 이 API가 하지 않는다.
- null 등 API 오용의 unchecked 예외를 IOException으로 변환하지 않는다.

## load 계약

- 입력 state와 in은 non-null이어야 한다. stream은 Java 객체 직렬화 데이터를 읽을 수 있는 위치에 있어야 한다.
- 첫 객체가 Collection이고 모든 원소가 Point인지 검사한다. String, null 컬렉션, null 원소, Point 아닌 원소는 IOException이다. 깨진 직렬화 데이터의 IOException도 전달된다. ClassNotFoundException은 IOException으로 변환된다.
- 검사가 모두 성공한 뒤에만 `state.replaceCells`를 호출한다. **Codec 자체는 실패 시 전달받은 state를 교체하지 않는다.** 다만 B는 호출 전에 보드를 clear하므로 전체 Load 실패 때 기존 보드가 보존되는 것은 아니다.
- x=열, y=행. 빈 컬렉션과 중복 좌표를 허용한다. 음수·보드 밖 좌표, 셀 수 상한, 파일 버전, 보드 크기·규칙은 검사하지 않는다.
- 읽은 컬렉션을 복사하지 않고 state에 전달한다. state의 raw Collection과 mutable Point는 A의 현재 모델 계약이다.
- 입력 스트림을 닫지 않는다. 첫 객체 뒤의 추가 객체/바이트가 없는지 검사하지 않는다. 역직렬화 전에 허용 타입을 제한하는 필터도 설정하지 않는다.

## flush 계약

- state와 out은 non-null이어야 한다. state에는 저장할 좌표가 이미 캡처되어 있어야 한다.
- `state.liveCells()`를 ObjectOutputStream으로 기록한다. Codec은 보드를 탐색하거나 좌표를 다시 수집하지 않는다.
- 전달된 컬렉션의 순서·중복을 그대로 쓴다. 정상 보드의 좌표당 1회 저장은 A의 createMemento와 B의 1회 캡처로 보장한다.
- 기록 실패는 IOException이다. 외부 출력 스트림에 대한 명시적 flush/close는 하지 않는다. 메서드 이름이 flush여도 일반 버퍼 스트림의 flush 계약과 같다고 가정하면 안 된다.
- 출력 도중 실패하면 일부 바이트가 남을 수 있다. 원자적 저장은 현재 계약이 아니다.

## 스레드 및 A/B/C/D 책임

C는 동기 API이며 별도 작업 스레드·잠금이 없다. 같은 state/stream을 동시에 공유할 때의 일관성을 보장하지 않는다. B가 Clock.stop을 호출한다는 사실만으로 진행 중 tick까지 끝났다고 보장할 수는 없다. 스냅샷 시점의 동기화는 A/B 합의 사항이다.

| 도메인 | 현재 소유 책임 | C와의 경계 |
| --- | --- | --- |
| A 모델 | Cell, NeighborhoodState, createMemento, transfer | C가 읽고 교체하는 데이터 타입 제공. 파일을 열지 않음 |
| B 실행 | stop, clear, 캡처·복원, 파일 처리 순서, 현재 stream close | C 네 API의 유일한 운영 직접 호출자 |
| C 저장·패턴 | 파일 stream 열기, 직렬화, 파일 구조 검증 | 보드 변경·Clock·UI 제어 없음 |
| D 화면 | FileDialogs, 메뉴, 오류 표시, repaint | B를 통해 저장/불러오기 실행 |

추가 계약과 패턴 설계는 [002 분석·제안](002-public-contract-and-refactoring.md)에 있다. 그 문서의 새 타입은 아직 구현되지 않았다.
