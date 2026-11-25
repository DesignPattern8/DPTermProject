package com.holub.life.ui;

import com.holub.life.application.GameController;
import javax.swing.JComponent;

/**
 * 1인용 암세포 게임 (cancer-feature-changes.md). 화면은 클래식과 같은 {@link BoardPanel}이고, {@link GameController}가
 * {@code CancerGame}을 갖는다는 점만 다르다. 이전에는 이 모드가 {@code Classic}이라는 이름이었다(ui/docs/010).
 */
public final class SinglePlayerMode implements GameMode {
  private final JComponent view;
  private final GameController controller;

  public SinglePlayerMode(JComponent view, GameController controller) {
    this.view = view;
    this.controller = controller;
  }

  @Override
  public String menuName() {
    return "Single Player";
  }

  @Override
  public JComponent view() {
    return view;
  }

  /** 1세대 진행 + 암세포 규칙. 메뉴 열림 검사는 {@link ModeSwitch}가 한다. */
  @Override
  public void tick() {
    controller.tick();
  }

  @Override
  public boolean supportsFileCommands() {
    return true;
  }
}
