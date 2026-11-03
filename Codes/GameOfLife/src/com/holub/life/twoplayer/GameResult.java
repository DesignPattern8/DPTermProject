package com.holub.life.twoplayer;

/** 게임 결과. scoreA·scoreB는 판정 시점의 셀 수다. */
public record GameResult(Outcome outcome, EndReason reason, int scoreA, int scoreB) {}
