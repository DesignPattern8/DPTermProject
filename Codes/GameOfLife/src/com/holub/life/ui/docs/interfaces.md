# 화면 추출 계약

- 제공 D / 사용 조립 지점·UI / 변경 2026-11-03, #24

```java
void BoardRenderer.redraw(Cell cell, Graphics g, Rectangle here, boolean drawAll)
Point GridCoordinateMapper.position(Cell cell, Point here, Rectangle surface)
void GridCoordinateMapper.userClicked(Cell cell, Point here, Rectangle surface)
```

redraw는 원본처럼 Resident/Neighborhood/DUMMY를 그린다. drawAll=false는 안정된 블록을 건너뛰고 안정화 직후 마지막 갱신을 소비한다. 전달받은 Graphics는 닫지 않고 필요한 복제본만 dispose한다. 좌표 변환은 원본의 재귀적 나눗셈과 예외를 보존한다. userClicked는 position 후 모델 toggle을 호출하는 기존 테스트·임시 호출부용 함수다. 새 UI는 position 결과를 제어 계층에 전달한다.

LifeFrame은 원본 창 제목·종료 동작·레이아웃을 유지한다. BoardPanel은 원본 크기 조정·paint·refreshNow를 소유한다. GameMenu.install은 Grid/Go 항목과 기존 Clock 리스너를 연결한다(→ #102에서 삭제, [011](011-remove-gamemenu-install.md)). FileDialogs.userSelected는 원본 JFileChooser 코드이며 취소도 FileNotFoundException으로 전달하여 기존 오류 대화상자를 유지한다.

# 메뉴 항목 표와 대화상자 (#9)

- 제공 D / 사용 `LifeFrame`(조립), 테스트 / 변경 2026-11-08, #9 ([005](005-menu-binding.md), [006](006-dialog-injection.md))
- 바뀐 것: 추가만 있다. `GameMenu.install(BoardPanel, GameController, Clock)`의 시그니처와 동작은 그대로다. (→ 2026-11-24 #102에서 `install` 삭제, [011](011-remove-gamemenu-install.md))

```java
public record GameMenu.Entry(String menu, String label, ActionListener action) {}
public static List<GameMenu.Entry> GameMenu.entries(
    GameController controller, Clock clock, UserDialogs dialogs, Runnable exit)

public interface UserDialogs {
  Optional<File> chooseFile(String selectButtonText);   // #57: 취소 = Optional.empty()
  void showError(String message);
}
public final class SwingUserDialogs implements UserDialogs
```

- `entries`: 변경할 수 없는 목록 10개를 원본 순서(Grid: Clear, Load, Store, Exit → Go: Halt, Tick (Single Step), Agonizing, Slow, Medium, Fast)로 돌려준다. 만들 때 Swing 창이나 `MenuSite`가 필요 없다. 항목 동작은 `ActionEvent`를 쓰지 않으므로 `null`로 실행해도 된다.
- Load/Store 동작: `dialogs.chooseFile("Load")`/`("Write")`로 고른 파일을 `controller.load`/`store`에 넘긴다. 고른 파일의 `IOException`(없는 파일, 형식 오류, 쓸 수 없는 경로)은 `dialogs.showError("Read Failed!")`/`("Write Failed!")`로 알린다.
- 선택 취소(#57, 2026-11-18 변경): `chooseFile`이 빈 `Optional`을 돌려주면 `controller`를 부르지 않고 오류창도 띄우지 않는다. 보드·실행 상태·파일·게임 알림 모두 그대로다. 원본(및 #46 이전 계약)은 취소도 `FileNotFoundException`으로 받아 실패 메시지를 띄웠다.
- Exit 동작: `exit.run()`. `install`은 `() -> System.exit(0)`을 넘긴다.
- `SwingUserDialogs.chooseFile`: `FileDialogs.userSelected(".", ".life", "Life File", text)`. `FileDialogs`가 "선택 없음"을 알리는 `FileNotFoundException`은 빈 `Optional`로 바꾼다(`FileDialogs` 자체 계약은 그대로). `showError`: `JOptionPane.showMessageDialog(null, message, "The Game of Life", ERROR_MESSAGE)`. 둘 다 Swing 이벤트 스레드에서 부른다(메뉴 동작에서 부르므로 지금은 항상 그렇다).

# 클릭 → `CellPosition` (#9)

- 제공 D / 사용 `BoardPanel` / 변경 2026-11-13, #9 ([008](008-board-api-boundary.md))
- 바뀐 것: 추가만 있다.

```java
public static CellPosition GridCoordinateMapper.cellAt(Cell cell, Point here, Rectangle surface)
```

- `position(cell, here, surface)`와 같은 계산(블록마다 정수 나눗셈)이고 결과만 `CellPosition(row = y, column = x)`로 돌려준다. 범위 밖 픽셀의 예외(`ArrayIndexOutOfBoundsException`)도 같다.
- `BoardPanel`은 이 값을 `GameController.toggle(row, column)`에 넘긴다.

# 결과 메시지와 암세포 화면 (#67)

- 제공 D / 사용 `LifeFrame`(조립), 테스트 / 변경 2026-11-14, #67 ([009](009-game-result-dialog.md))
- 바뀐 것: `UserDialogs`에 메서드 추가(구현체는 D 안의 `SwingUserDialogs`와 테스트 대역뿐), 클래스 추가. 다른 도메인 시그니처 변경 없음.

```java
public interface UserDialogs {
  ...
  void showMessage(String message);          // 추가
}
public SwingUserDialogs(Component messageParent)   // 추가. 기존 무인자 생성자는 messageParent = null
public final class GameResultAnnouncer implements GameListener {
  public GameResultAnnouncer(GameController controller, UserDialogs dialogs, Executor onUiThread)
}
```

- `GameResultAnnouncer.boardChanged`: `GAME_OVER`일 때만 알림을 받은 스레드에서 `controller.gameResultMessage()`를 읽고, `onUiThread`에 `dialogs.showMessage(message)`를 넘긴다. 다른 알림은 무시한다. `LifeFrame`은 `SwingUtilities::invokeLater`를 넘긴다.
- `SwingUserDialogs.showMessage`: `JOptionPane.showMessageDialog(messageParent, message, "Game of Life", INFORMATION_MESSAGE)`. 이벤트 스레드에서 부른다. `showError`는 계속 부모 없이(`null`) 띄운다(원본).
- 사용하는 다른 도메인 API(#60): B `GameController.treat(int, int)`, `GameController.gameResultMessage()`, `GameListener.Change.GAME_OVER`, A `Cell.isCancer()`.

# 게임 모드 전환 (`GameMode`, `ModeSwitch`, `ClassicMode`)

- 제공 ui / 사용 조립 지점(`LifeFrame`, `Life`), 2인용(`twoplayer.ui.TwoPlayerMode`) / 담당 Son2kwon
- 변경 이력: 2026-11-16, 최초 작성 (#7). 설계 근거: [twoplayer/007](../../twoplayer/docs/007-two-player-ui.md)

```java
public interface GameMode {
  String menuName();
  JComponent view();
  void tick();
  boolean supportsFileCommands();
}
public final class ModeSwitch {
  public ModeSwitch(Clock clock, GameMode first, Consumer<Boolean> fileCommandsEnabled);
  public void add(GameMode mode);
  public void select(GameMode mode);   // EDT에서
  public GameMode current();
  public JComponent view();            // CardLayout. 크기는 지금 모드 화면을 따른다
}
public final class ClassicMode implements GameMode {
  public ClassicMode(JComponent view, GameController controller);  // "Classic", tick = controller.tick(), 파일 명령 true
}
// 추가: GameMenu.installMenus(panel, controller, clock), GameMenu.setFileCommandsEnabled(panel, enabled)
// 추가: LifeFrame(Cell, GameController, Clock, List<GameMode> otherModes)
```

- `ModeSwitch`는 만들 때 시계에 리스너를 하나 단다. tick마다 메뉴가 열려 있으면 건너뛰고(기존 `onTick`과 같음), 아니면 잠금 안에서 지금 모드의 `tick()`만 부른다.
- `select`: 시계를 멈추고 → 잠금을 잡아(진행 중인 tick이 끝나길 기다림) 지금 모드를 바꾸고 → 카드를 바꾸고 창을 `pack()`하고 → `fileCommandsEnabled`에 새 모드의 값을 넘긴다. 등록하지 않은 모드면 `IllegalArgumentException`.
- 모든 모드의 tick과 전환이 한 잠금 안에서 돌므로 두 모드의 보드가 동시에 세대를 진행하지 않는다. #74에서 `Neighborhood`의 읽기 허가·전이 깊이는 보드별로 분리했으며, 이 앱 실행 정책은 그대로 유지한다. [A 모델 계약](../../model/docs/interfaces.md)
- `GameMode.tick()`은 시계 스레드(Go → Tick이면 EDT)에서 잠금을 잡은 채 불린다. 오래 걸리면 모드 전환이 기다린다. 다른 스레드를 기다리는 일(예: `invokeAndWait`)을 하면 안 된다.
- ~~`GameMenu.install`은 원래 동작 그대로다(Grid·Go 메뉴 + 클래식 tick 리스너).~~ → 2026-11-24 #102에서 삭제([011](011-remove-gamemenu-install.md)). 시계 리스너는 `ModeSwitch`만 단다. `LifeFrame`은 `installMenus` + `ModeSwitch`를 쓴다. `otherModes`가 비어 있으면 Mode 메뉴를 달지 않는다.
- `setFileCommandsEnabled`는 Grid의 Clear·Load·Store만 켜고 끈다. Exit은 그대로.

# 프리셋 선택·배치 (#62)

- 제공 D / 사용 `LifeFrame`(조립), 테스트 / 변경 2026-11-24, #62 ([010](010-preset-placement-ui.md))
- 사용하는 B API: `GameController.presets()`, `GameController.applyPreset(String, CellPosition)`, `PresetChoice`, `PresetResult` (PR #99, [application/008](../../application/docs/008-preset-placement.md))
- 바뀐 것: 추가만 있다. 기존 `BoardPanel(Cell, GameController)`, `GameMenu.installMenus(BoardPanel, GameController, Clock)`는 그대로(프리셋 없이 동작).

```java
public final class PresetPlacement {
  public PresetPlacement(GameController controller, UserDialogs dialogs)
  public void addWaitingListener(Consumer<Boolean> listener)
  public void select(String id)
  public void cancel()
  public boolean isWaiting()
  public boolean click(CellPosition cell, boolean rightButton)
}
public BoardPanel(Cell outermostCell, GameController controller, PresetPlacement presets)
public static List<GameMenu.Entry> GameMenu.presetEntries(GameController controller, PresetPlacement placement)
public static void GameMenu.installMenus(BoardPanel panel, GameController controller, Clock clock, PresetPlacement placement)
public static void GameMenu.setPresetsEnabled(BoardPanel panel, GameController controller, boolean enabled)
```

- `presetEntries`: 메뉴 `Preset`, 라벨은 `PresetChoice.displayName()`, 순서는 `controller.presets()` 순서. 동작은 `placement.select(id)`.
- `PresetPlacement.click`: 대기 중이 아니면 `false`(호출한 쪽이 평소대로 `toggle`/`treat`). 대기 중이면 대기를 끝내고 `true`를 돌려준다. 왼쪽 클릭은 `controller.applyPreset(id, cell)`을 부르고, `OUT_OF_BOUNDS`면 `"Preset does not fit here."`, `UNKNOWN_ID`면 `"Unknown preset."`를 `dialogs.showError`로 표시한다. 오른쪽 클릭은 취소만 한다(치료하지 않음).
- `select`는 대기를 시작(이미 대기 중이면 프리셋만 바꿈), `cancel`은 대기 중일 때만 끝낸다. 대기 리스너는 시작 시 `true`, 끝날 때 `false`를 받는다. `BoardPanel`은 이것으로 커서를 십자/기본으로 바꾼다.
- `installMenus(…, placement)`: Grid·Go 항목을 고르면 먼저 `placement.cancel()`을 부른다. `Preset` 메뉴의 requester는 패널이다.
- 스레드: 모두 Swing 이벤트 스레드에서 부른다(메뉴·마우스 처리).
