package com.holub.life.ui;

import com.holub.life.application.GameController;
import javax.swing.JComponent;

/**
 * 원본(Holub) Life 그대로의 클래식 모드. 암세포 규칙이 없는 {@link GameController}(B3/S23)와 {@link BoardPanel}을 {@link
 * GameMode}로 묶는다. 동작은 바꾸지 않는다. 암세포 게임은 {@link SinglePlayerMode}다(ui/docs/010).
 */
public final class ClassicMode implements GameMode {
  private final JComponent view;
  private final GameController controller;

  public ClassicMode(JComponent view, GameController controller) {
    this.view = view;
    this.controller = controller;
  }

  @Override
  public String menuName() {
    return "Classic";
  }

  @Override
  public JComponent view() {
    return view;
  }

  /** 기존 {@code GameMenu.onTick}과 같은 1세대 진행. 메뉴 열림 검사는 {@link ModeSwitch}가 한다. */
  @Override
  public void tick() {
    controller.tick();
  }

  @Override
  public boolean supportsFileCommands() {
    return true;
  }
}
