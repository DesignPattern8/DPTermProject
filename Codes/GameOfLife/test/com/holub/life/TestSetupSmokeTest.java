package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 테스트 환경(Gradle + JUnit 5)이 기존 src의 클래스를 컴파일·로드할 수 있는지만 확인한다. */
class TestSetupSmokeTest {

  @Test
  @DisplayName("테스트 환경에서 기존 소스의 Resident를 만들 수 있고, 새 셀은 죽어 있다")
  void newResidentIsDeadInTestEnvironment() {
    // Given / When
    Cell cell = new Resident();

    // Then
    assertFalse(cell.isAlive());
  }
}
