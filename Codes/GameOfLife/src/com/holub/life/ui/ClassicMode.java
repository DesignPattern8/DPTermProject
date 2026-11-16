package com.holub.life.ui;

import com.holub.life.application.GameController;
import javax.swing.JComponent;

/** 기존 Life 화면(BoardPanel)과 {@link GameController}를 {@link GameMode}로 묶는다. 동작은 바꾸지 않는다. */
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
