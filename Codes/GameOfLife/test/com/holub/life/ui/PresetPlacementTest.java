package com.holub.life.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.application.*;
import com.holub.life.model.*;
import java.io.File;
import java.util.*;
import org.junit.jupiter.api.Test;

/** UI-R21~R28 (#62, decision 010): choosing and placing a preset. See ui/docs/test-cases.md. */
class PresetPlacementTest {
  private static class IdleTickSource implements TickSource {
    @Override
    public void start(int intervalMillis, Runnable onTick) {}

    @Override
    public void stop() {}
  }

  /** Never starts new cancer (same idea as #60's FixedRandom). */
  private static class NoNewCancer extends Random {
    @Override
    public double nextDouble() {
      return 0.99;
    }
  }

  /** Records shown errors; file dialogs are not used here. */
  private static class RecordingDialogs implements UserDialogs {
    final List<String> errors = new ArrayList<>();

    @Override
    public Optional<File> chooseFile(String selectButtonText) {
      throw new AssertionError("not used");
    }

    @Override
    public void showError(String message) {
      errors.add(message);
    }

    @Override
    public void showMessage(String message) {
      throw new AssertionError("not used");
    }
  }

  private static Resident cellAt(Neighborhood board, int row, int column) {
    Neighborhood block = (Neighborhood) board.childAt(row / 8, column / 8);
    return (Resident) block.childAt(row % 8, column % 8);
  }

  private static Set<CellPosition> glider(int row, int column) {
    return Set.of(
        new CellPosition(row, column + 1),
        new CellPosition(row + 1, column + 2),
        new CellPosition(row + 2, column),
        new CellPosition(row + 2, column + 1),
        new CellPosition(row + 2, column + 2));
  }

  private final Neighborhood cells = new Neighborhood(8, new Neighborhood(8, new Resident()));
  private final CompositeBoard board = new CompositeBoard(cells);
  private final GameController controller =
      new GameController(board, new Clock(new IdleTickSource()));
  private final RecordingDialogs dialogs = new RecordingDialogs();
  private final PresetPlacement placement = new PresetPlacement(controller, dialogs);

  private void choose(String label) {
    for (GameMenu.Entry entry : GameMenu.presetEntries(controller, placement))
      if (entry.label().equals(label)) {
        entry.action().actionPerformed(null);
        return;
      }
    fail("Missing preset entry: " + label);
  }

  @Test
  void presetMenuFollowsCatalogOrder() {
    List<String> actual = new ArrayList<>();
    for (GameMenu.Entry entry : GameMenu.presetEntries(controller, placement))
      actual.add(entry.menu() + "/" + entry.label());
    assertEquals(List.of("Preset/Block", "Preset/Blinker", "Preset/Glider"), actual);
  }

  @Test
  void leftClickPlacesSelectedPreset() {
    choose("Glider");
    assertTrue(placement.click(new CellPosition(10, 10), false));
    assertEquals(glider(10, 10), board.snapshot().liveCells());
    assertTrue(dialogs.errors.isEmpty());
    assertFalse(placement.isWaiting());
  }

  @Test
  void clicksPassThroughWhenNotWaiting() {
    assertFalse(placement.click(new CellPosition(10, 10), false));
    assertFalse(placement.click(new CellPosition(10, 10), true));
    assertTrue(board.snapshot().liveCells().isEmpty());
  }

  @Test
  void rightClickCancelsWithoutTreating() {
    cellAt(cells, 25, 12).setCancer();
    CancerGame cancer = new CancerGame(cells, new NoNewCancer());
    GameController cancerController =
        new GameController(board, new Clock(new IdleTickSource()), cancer);
    PresetPlacement cancerPlacement = new PresetPlacement(cancerController, dialogs);
    cancerPlacement.select("glider");

    assertTrue(cancerPlacement.click(new CellPosition(25, 12), true));

    assertFalse(cancerPlacement.isWaiting());
    assertTrue(cellAt(cells, 25, 12).isCancer());
    assertEquals(CancerGame.INITIAL_TREATMENT_CHARGES, cancer.treatmentCharges());
  }

  @Test
  void cancelReturnsToNormalClicks() {
    choose("Glider");
    placement.cancel();
    assertFalse(placement.click(new CellPosition(10, 10), false));
    assertTrue(board.snapshot().liveCells().isEmpty());
  }

  @Test
  void outOfBoundsShowsErrorAndEndsWaiting() {
    choose("Glider");
    assertTrue(placement.click(new CellPosition(63, 63), false));
    assertEquals(List.of("Preset does not fit here."), dialogs.errors);
    assertTrue(board.snapshot().liveCells().isEmpty());
    assertFalse(placement.isWaiting());
  }

  @Test
  void unknownIdShowsError() {
    placement.select("nope");
    assertTrue(placement.click(new CellPosition(10, 10), false));
    assertEquals(List.of("Unknown preset."), dialogs.errors);
    assertTrue(board.snapshot().liveCells().isEmpty());
    assertFalse(placement.isWaiting());
  }

  @Test
  void waitingChangesAreReported() {
    List<Boolean> changes = new ArrayList<>();
    placement.addWaitingListener(changes::add);
    choose("Glider");
    placement.click(new CellPosition(10, 10), false);
    choose("Glider");
    placement.cancel();
    assertEquals(List.of(true, false, true, false), changes);
  }
}
