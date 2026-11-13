package com.holub.life.ui;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * The dialogs the Grid menu needs (Strategy). {@link SwingUserDialogs} shows the original Swing
 * dialogs; tests pass a recording stand-in so menu actions run without a window.
 */
public interface UserDialogs {
  /**
   * Ask the user for a Life file.
   *
   * @param selectButtonText text of the chooser's select button ("Load" or "Write")
   * @throws FileNotFoundException if the user didn't select a file (cancel)
   */
  File chooseFile(String selectButtonText) throws FileNotFoundException;

  /** Tell the user that an operation failed. */
  void showError(String message);
}
