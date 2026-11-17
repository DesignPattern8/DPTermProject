package com.holub.life.persistence;

import com.holub.life.model.CellPosition;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Built-in classic patterns, in stable menu order and relative to their top-left origin. */
public final class PresetCatalog {
  private static final List<PatternPreset> PRESETS =
      List.of(
          preset(
              "block",
              "Block",
              new CellPosition(0, 0),
              new CellPosition(0, 1),
              new CellPosition(1, 0),
              new CellPosition(1, 1)),
          preset(
              "blinker",
              "Blinker",
              new CellPosition(0, 0),
              new CellPosition(0, 1),
              new CellPosition(0, 2)),
          preset(
              "glider",
              "Glider",
              new CellPosition(0, 1),
              new CellPosition(1, 2),
              new CellPosition(2, 0),
              new CellPosition(2, 1),
              new CellPosition(2, 2)));

  public List<PatternPreset> list() {
    return PRESETS;
  }

  public Optional<PatternPreset> find(String id) {
    return PRESETS.stream().filter(preset -> preset.id().equals(id)).findFirst();
  }

  private static PatternPreset preset(String id, String displayName, CellPosition... cells) {
    return new PatternPreset(id, displayName, new PatternData(List.of(cells), Set.of()));
  }
}
