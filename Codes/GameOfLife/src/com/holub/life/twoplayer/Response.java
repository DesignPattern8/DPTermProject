package com.holub.life.twoplayer;

/** 플레이어 입력의 결과. ACCEPTED가 아니면 거부 이유다 (004-player-input.md). */
public enum Response {
  ACCEPTED,
  /** 지금 단계에서는 받지 않는 입력 */
  WRONG_PHASE,
  /** 현재 차례가 아닌 플레이어의 입력 */
  NOT_YOUR_TURN,
  /** 보드 밖 좌표 */
  OUT_OF_BOARD,
  /** 이미 살아 있는 칸 */
  CELL_NOT_EMPTY,
  /** 초기 배치에서 상대 진영 */
  OPPONENT_HALF,
  /** 임시 배치 없이 확정 */
  NO_TENTATIVE_PLACEMENT,
  /** 패스할 수 없는 단계(초기 배치) */
  PASS_NOT_ALLOWED
}
