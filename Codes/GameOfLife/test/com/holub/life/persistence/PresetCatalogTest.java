package com.holub.life.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.CellPosition;
import java.util.List;
import org.junit.jupiter.api.Test;

class PresetCatalogTest {
  @Test
  void returnsStableImmutablePresets() {
    var catalog = new PresetCatalog();
    assertEquals(
        List.of("block", "blinker", "glider"),
        catalog.list().stream().map(PatternPreset::id).toList());
    assertEquals(
        List.of(
            new CellPosition(0, 0),
            new CellPosition(0, 1),
            new CellPosition(1, 0),
            new CellPosition(1, 1)),
        catalog.find("block").orElseThrow().data().cells());
    assertEquals(
        List.of(new CellPosition(0, 0), new CellPosition(0, 1), new CellPosition(0, 2)),
        catalog.find("blinker").orElseThrow().data().cells());
    assertEquals(
        List.of(
            new CellPosition(0, 1),
            new CellPosition(1, 2),
            new CellPosition(2, 0),
            new CellPosition(2, 1),
            new CellPosition(2, 2)),
        catalog.find("glider").orElseThrow().data().cells());
    assertThrows(UnsupportedOperationException.class, () -> catalog.list().clear());
    for (var preset : catalog.list()) {
      assertFalse(preset.displayName().isBlank());
      assertTrue(preset.data().cancerCells().isEmpty());
    }
  }

  @Test
  void rejectsNullPresetFields() {
    var data = new PresetCatalog().list().getFirst().data();
    assertThrows(NullPointerException.class, () -> new PatternPreset(null, "Block", data));
    assertThrows(NullPointerException.class, () -> new PatternPreset("block", null, data));
    assertThrows(NullPointerException.class, () -> new PatternPreset("block", "Block", null));
  }

  @Test
  void returnsEmptyForUnknownId() {
    assertTrue(new PresetCatalog().find("unknown").isEmpty());
    assertTrue(new PresetCatalog().find(null).isEmpty());
  }
}
