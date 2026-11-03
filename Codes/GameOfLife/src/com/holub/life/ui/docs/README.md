# D 도메인: 화면·사용자 입력 (`com.holub.life.ui`)

- 담당: 병국
- 핵심 질문: 어떻게 보여주고 입력받는가?
- 원칙: 화면은 **확정된 `BoardSnapshot`을 받아 그리기만** 하고, 입력은 **`CellPosition`으로 바꿔 `GameController`(B)에 요청만** 한다. 화면 코드는 규칙을 계산하거나 모델을 수정하지 않는다.

## 기존 코드에서 가져온 책임

| 기존 위치 | 새 위치 |
| --- | --- |
| `Universe` 화면 그리기·마우스 처리·크기 조정 (`life/Universe.java`) | `BoardPanel`, `GridCoordinateMapper` |
| `Resident.redraw` (`life/Resident.java:85`) | `CellLayerRenderer` |
| `Neighborhood.redraw`의 테두리·활성 표시 (`life/Neighborhood.java:286`) | `BlockBorderDecorator`, `ActiveBlockDecorator` |
| `Neighborhood.userClicked`의 픽셀 해석 | `GridCoordinateMapper.toCell` |
| `Universe` 생성자의 Grid 메뉴, `Clock.createMenus()`의 Go 메뉴 | `GameMenu` |
| `io/Files.userSelected`(JFileChooser), `Universe`의 오류 다이얼로그 | `FileDialogs` |
| `Clock`의 `java.util.Timer` | `SwingTickSource` (B의 `TickSource` 구현) |
| `Life` 생성자의 창 구성 | `LifeFrame` |

## 현재 상태 (1단계)

- 위 클래스는 A·B 인터페이스를 **호출하는 형태로 작성 완료**. A·B 인터페이스 파일이 들어오기 전에는 컴파일되지 않는다.
- `Life.java`, `Universe.java`는 아직 바꾸지 않았다. 기존 앱 실행 경로는 그대로다.
- 실측과 테스트 실행은 A·B 구현과 JUnit 설정이 들어온 뒤(2단계)에 한다.

## 문서 목록

| 문서 | 내용 | 상태 |
| --- | --- | --- |
| [interfaces.md](interfaces.md) | D가 호출하는 A·B 인터페이스 요청안 | 검토 요청 |
| [test-cases.md](test-cases.md) | 테스트 케이스 정의 | 정의 완료, 코드 작성 전 |
| [001-screen-update.md](001-screen-update.md) | 화면 갱신 방식 (Observer) | 제안 |
| [002-board-rendering.md](002-board-rendering.md) | 보드 렌더링 구조 (Decorator) | 제안 |
| [003-input-binding.md](003-input-binding.md) | 메뉴·입력과 동작 연결 | 제안 |
| [004-swing-tick-source.md](004-swing-tick-source.md) | 타이머 구현 (Swing Timer) | 제안 |
