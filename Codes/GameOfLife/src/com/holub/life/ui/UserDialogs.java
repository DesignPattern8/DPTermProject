package com.holub.life.ui;

import java.io.File;
import java.util.Optional;

/**
 * The dialogs the UI needs: the Grid menu's file chooser and error box, and the game result
 * message (Strategy). {@link SwingUserDialogs} shows the original Swing
 * dialogs; tests pass a recording stand-in so menu actions run without a window.
 */
public interface UserDialogs {
  /**
   * Ask the user for a Life file.
   *
   * @param selectButtonText text of the chooser's select button ("Load" or "Write")
   * @return the chosen file, or empty if the user cancelled. Cancelling is not an error (#57).
   */
  Optional<File> chooseFile(String selectButtonText);

  /** Tell the user that an operation failed. */
  void showError(String message);

  /** Tell the user something that is not an error, such as the cancer game's result. */
  void showMessage(String message);
}
