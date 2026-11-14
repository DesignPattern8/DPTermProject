package com.holub.life.ui;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.model.Cell;
import com.holub.ui.MenuSite;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Existing Life window configuration. */
public final class LifeFrame extends JFrame {
  public LifeFrame(Cell board, GameController controller, Clock clock) {
    super("The Game of Life. " + "(c)2003 Allen I. Holub <http://www.holub.com>");
    MenuSite.establish(this);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());
    BoardPanel panel = new BoardPanel(board, controller);
    GameMenu.install(panel, controller, clock);
    getContentPane().add(panel, BorderLayout.CENTER);
    controller.addGameListener(
        new GameResultAnnouncer(controller, new SwingUserDialogs(this), SwingUtilities::invokeLater));
    pack();
    setVisible(true);
  }
}
