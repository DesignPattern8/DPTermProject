package com.holub.life.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.application.*;
import com.holub.life.model.*;
import java.awt.Point;
import java.util.*;
import java.util.Timer;
import javax.swing.*;
import org.junit.jupiter.api.Test;

class GameMenuTest {
  private static class RecordingTimer extends Timer {
    TimerTask task;
    long period;

    RecordingTimer() {
      super(true);
    }

    @Override
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
      this.task = task;
      this.period = period;
      super.scheduleAtFixedRate(task, 86_400_000, period);
    }
  }

  @Test
  void goCommandsKeepOriginalActions() {
    RecordingTimer timer = new RecordingTimer();
    try {
      Clock clock = new Clock(new UtilTimerTickSource(timer));
      String[] names = {"Agonizing", "Slow", "Medium", "Fast"};
      long[] periods = {500, 150, 70, 30};
      for (int i = 0; i < names.length; i++) {
        GameMenu.go(clock, names[i]);
        assertEquals(periods[i], timer.period);
      }
      GameMenu.go(clock, "Halt");
      assertFalse(timer.task.cancel());
      int[] ticks = {0};
      clock.addClockListener(() -> ticks[0]++);
      GameMenu.go(clock, "Tick (Single Step)");
      assertEquals(1, ticks[0]);
    } finally {
      timer.cancel();
    }
  }

  @Test
  void openMenuSuppressesTick() {
    RecordingTimer timer = new RecordingTimer();
    MenuSelectionManager menus = MenuSelectionManager.defaultManager();
    try {
      Cell board = new Neighborhood(8, new Neighborhood(8, new Resident()));
      board.toggle(10, 9);
      board.toggle(10, 10);
      board.toggle(10, 11);
      Clock clock = new Clock(new UtilTimerTickSource(timer));
      GameController controller = new GameController(board, clock);
      menus.setSelectedPath(new MenuElement[] {new JMenu("Go")});
      GameMenu.onTick(controller);
      assertTrue(board.createMemento().isAlive(new Point(9, 10)));
      menus.clearSelectedPath();
      GameMenu.onTick(controller);
      assertFalse(board.createMemento().isAlive(new Point(9, 10)));
      assertTrue(board.createMemento().isAlive(new Point(10, 9)));
    } finally {
      menus.clearSelectedPath();
      timer.cancel();
    }
  }
}
