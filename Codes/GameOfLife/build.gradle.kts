plugins {
    java
}

repositories {
    mavenCentral()
}

// IntelliJ 모듈 설정(GameOfLife.iml)은 그대로 두고, 기존 디렉터리를 그대로 소스 루트로 쓴다.
sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(emptyList<String>())
    }
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(emptyList<String>())
    }
}

// 저장소에 커밋된 IntelliJ 설정(.idea/misc.xml, JDK_26)과 같은 JDK로 컴파일·테스트한다.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // CI에는 디스플레이가 없다. 모델 테스트는 Swing 창을 띄우지 않아야 한다(AGENTS.md 3-1).
    systemProperty("java.awt.headless", "true")
    // 원본 코드의 assert(Resident.verify 등)를 테스트에서 켠다. Gradle 기본값이지만 의도를 드러내려고 명시한다.
    enableAssertions = true
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
