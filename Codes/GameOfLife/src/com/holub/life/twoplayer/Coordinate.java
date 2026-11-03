package com.holub.life.twoplayer;

/** 보드의 칸 좌표 (행, 열), 0부터 센다. 게임 모델이 java.awt에 의존하지 않도록 따로 둔다. */
public record Coordinate(int row, int column) {}
