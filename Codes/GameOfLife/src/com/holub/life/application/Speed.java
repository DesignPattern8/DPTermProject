package com.holub.life.application;

/** The Go menu running speeds, as milliseconds between generations. Halt is {@link Clock#stop()}. */
public enum Speed {
  AGONIZING(500),
  SLOW(150),
  MEDIUM(70),
  FAST(30);

  private final int millis;

  Speed(int millis) {
    this.millis = millis;
  }

  public int millis() {
    return millis;
  }
}
