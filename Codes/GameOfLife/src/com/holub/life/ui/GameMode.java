package com.holub.life.ui;

import javax.swing.JComponent;

/**
 * LifeFrame의 Mode 메뉴로 고를 수 있는 게임 모드 (Strategy, twoplayer/docs/007-two-player-ui.md 결정 1). 모드마다 다른 것만
 * 담는다: 화면, 시계 tick 1번의 동작, 파일 명령(Clear·Load·Store) 사용 여부.
 */
public interface GameMode {
  /** Mode 메뉴 항목 이름. */
  String menuName();

  /** 이 모드의 화면. {@link ModeSwitch}가 CardLayout 카드로 붙인다. */
  JComponent view();

  /**
   * 시계 tick 1번. 이 모드가 선택되어 있고 메뉴가 닫혀 있을 때만 {@link ModeSwitch}가 부른다. 시계 스레드(또는 Go 메뉴의 Tick이면
   * EDT)에서 불리고, 모든 모드의 tick과 모드 전환은 같은 잠금 안에서 하나씩만 돈다.
   */
  void tick();

  /** Grid 메뉴의 Clear·Load·Store를 이 모드에서 쓸 수 있는가. */
  boolean supportsFileCommands();
}
