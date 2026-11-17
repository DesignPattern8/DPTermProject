package com.holub.life.ui;

import com.holub.life.application.GameController;
import com.holub.life.application.GameListener;
import java.util.concurrent.Executor;

/**
 * Shows the cancer game's result when the controller reports {@link GameListener.Change#GAME_OVER}
 * (Observer). The message is read on the notifying thread and shown through {@code onUiThread},
 * which is {@code SwingUtilities::invokeLater} in the application.
 */
public final class GameResultAnnouncer implements GameListener {
  private final GameController controller;
  private final UserDialogs dialogs;
  private final Executor onUiThread;

  public GameResultAnnouncer(GameController controller, UserDialogs dialogs, Executor onUiThread) {
    this.controller = controller;
    this.dialogs = dialogs;
    this.onUiThread = onUiThread;
  }

  @Override
  public void boardChanged(Change change) {
    if (change != Change.GAME_OVER) return;
    String message = controller.gameResultMessage();
    onUiThread.execute(() -> dialogs.showMessage(message));
  }
}
