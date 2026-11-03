package com.holub.life.twoplayer;

/** 게임이 끝난 이유 (rules.md 6장). */
public enum EndReason {
  /** 시뮬레이션 중 한쪽 또는 양쪽 셀 수가 0 */
  WIPE_OUT,
  /** R번째 라운드의 시뮬레이션 완료 */
  ROUNDS_COMPLETED
}
