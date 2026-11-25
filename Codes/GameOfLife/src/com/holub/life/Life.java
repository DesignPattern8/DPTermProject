package com.holub.life;

import com.holub.life.application.CancerGame;
import com.holub.life.application.Clock;
import com.holub.life.application.GameController;
import com.holub.life.application.UtilTimerTickSource;
import com.holub.life.model.CompositeBoard;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;
import com.holub.life.persistence.BoardFileStore;
import com.holub.life.persistence.LegacyLifeCodec;
import com.holub.life.twoplayer.ui.TwoPlayerMode;
import com.holub.life.ui.LifeFrame;
import java.util.List;
import java.util.Random;
import java.util.Timer;

/** Application entry point and assembly of the existing domains. */
public final class Life {
  private Life() {}

  public static void main(String[] arguments) {
    Clock clock = new Clock(new UtilTimerTickSource(new Timer()));
    BoardFileStore files = new BoardFileStore(new LegacyLifeCodec());

    // 클래식: 원본 Holub Life (B3/S23, 암세포 없음)
    Neighborhood classicBoard = new Neighborhood(8, new Neighborhood(8, new Resident()));
    GameController classic =
        new GameController(new CompositeBoard(classicBoard), classicBoard, clock, null, files);

    // 1인용: 암세포 게임
    Neighborhood singleBoard = new Neighborhood(8, new Neighborhood(8, new Resident()));
    GameController single =
        new GameController(
            new CompositeBoard(singleBoard),
            singleBoard,
            clock,
            new CancerGame(singleBoard, new Random()),
            files);

    new LifeFrame(classicBoard, classic, singleBoard, single, clock, List.of(new TwoPlayerMode()));
  }
}
