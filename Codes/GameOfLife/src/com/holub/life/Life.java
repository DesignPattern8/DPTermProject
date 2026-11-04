package com.holub.life;

import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.model.Cell;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;
import com.holub.life.ui.LifeFrame;

/** Application entry point and assembly of the existing domains. */
public final class Life {
  private Life() {}

  public static void main(String[] arguments) {
    Cell board = new Neighborhood(8, new Neighborhood(8, new Resident()));
    Clock clock = Clock.instance();
    GameController controller = new GameController(board, clock);
    new LifeFrame(board, controller, clock);
  }
}
