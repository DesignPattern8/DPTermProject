# 화면 추출 계약

- 제공 D / 사용 조립 지점·UI / 변경 2026-11-03, #24

```java
void BoardRenderer.redraw(Cell cell, Graphics g, Rectangle here, boolean drawAll)
Point GridCoordinateMapper.position(Cell cell, Point here, Rectangle surface)
void GridCoordinateMapper.userClicked(Cell cell, Point here, Rectangle surface)
```

redraw는 원본처럼 Resident/Neighborhood/DUMMY를 그린다. drawAll=false는 안정된 블록을 건너뛰고 안정화 직후 마지막 갱신을 소비한다. 전달받은 Graphics는 닫지 않고 필요한 복제본만 dispose한다. 좌표 변환은 원본의 재귀적 나눗셈과 예외를 보존한다. userClicked는 position 후 모델 toggle을 호출하는 기존 테스트·임시 호출부용 함수다. 새 UI는 position 결과를 제어 계층에 전달한다.

LifeFrame은 원본 창 제목·종료 동작·레이아웃을 유지한다. BoardPanel은 원본 크기 조정·paint·refreshNow를 소유한다. GameMenu.install은 Grid/Go 항목과 기존 Clock 리스너를 연결한다. FileDialogs.userSelected는 원본 JFileChooser 코드이며 취소도 FileNotFoundException으로 전달하여 기존 오류 대화상자를 유지한다.

# 메뉴 항목 표와 대화상자 (#9)

- 제공 D / 사용 `LifeFrame`(조립), 테스트 / 변경 2026-11-08, #9 ([005](005-menu-binding.md), [006](006-dialog-injection.md))
- 바뀐 것: 추가만 있다. `GameMenu.install(BoardPanel, GameController, Clock)`의 시그니처와 동작은 그대로다.

```java
public record GameMenu.Entry(String menu, String label, ActionListener action) {}
public static List<GameMenu.Entry> GameMenu.entries(
    GameController controller, Clock clock, UserDialogs dialogs, Runnable exit)

public interface UserDialogs {
  File chooseFile(String selectButtonText) throws FileNotFoundException;
  void showError(String message);
}
public final class SwingUserDialogs implements UserDialogs
```

- `entries`: 변경할 수 없는 목록 10개를 원본 순서(Grid: Clear, Load, Store, Exit → Go: Halt, Tick (Single Step), Agonizing, Slow, Medium, Fast)로 돌려준다. 만들 때 Swing 창이나 `MenuSite`가 필요 없다. 항목 동작은 `ActionEvent`를 쓰지 않으므로 `null`로 실행해도 된다.
- Load/Store 동작: `dialogs.chooseFile("Load")`/`("Write")`로 고른 파일을 `controller.load`/`store`에 넘긴다. 선택 취소(`FileNotFoundException`)를 포함한 `IOException`은 `dialogs.showError("Read Failed!")`/`("Write Failed!")`로 알린다. 원본 동작이다.
- Exit 동작: `exit.run()`. `install`은 `() -> System.exit(0)`을 넘긴다.
- `SwingUserDialogs.chooseFile`: `FileDialogs.userSelected(".", ".life", "Life File", text)`. `showError`: `JOptionPane.showMessageDialog(null, message, "The Game of Life", ERROR_MESSAGE)`. 둘 다 Swing 이벤트 스레드에서 부른다(메뉴 동작에서 부르므로 지금은 항상 그렇다).
