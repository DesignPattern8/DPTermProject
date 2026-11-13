package com.holub.life;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.application.UtilTimerTickSource;
import com.holub.life.model.CompositeBoard;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;
import com.holub.life.ui.LifeFrame;
import java.util.Timer;

/** Application entry point and assembly of the existing domains. */
public final class Life {
  private Life() {}

  public static void main(String[] arguments) {
    Neighborhood board = new Neighborhood(8, new Neighborhood(8, new Resident()));
    Clock clock = new Clock(new UtilTimerTickSource(new Timer()));
    GameController controller = new GameController(new CompositeBoard(board), board, clock);
    new LifeFrame(board, controller, clock);
  }
}
