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

// ---- 다이어그램 렌더링 (AGENTS.md 2-5) ----
// src/**/docs/diagrams/*.puml 을 같은 폴더에 png·svg로 렌더링한다: ./gradlew renderDiagrams
// 리팩토링 전 원본 다이어그램(저장소 루트의 diagrams/)은 보존 대상이라 렌더링하지 않는다.
// Graphviz 없이도 상태·클래스·시퀀스 다이어그램이 렌더링되는 것을 확인했다(이슈 #22). 레이아웃이 깨지면 .puml에 `!pragma layout smetana`를 넣는다.
// 결과 파일 이름은 `@startuml <이름>`을 따르므로 이름을 파일명과 같게 쓴다.
val plantuml: Configuration = configurations.create("plantuml")

dependencies {
    plantuml("net.sourceforge.plantuml:plantuml:1.2026.9")
}

val diagramSources = fileTree("src") { include("**/docs/diagrams/*.puml") }

fun registerDiagramRender(name: String, format: String) =
    tasks.register<JavaExec>(name) {
        group = "documentation"
        description = "Render src/**/docs/diagrams/*.puml to $format next to each source"
        classpath = plantuml
        mainClass.set("net.sourceforge.plantuml.Run")
        jvmArgs("-Djava.awt.headless=true")
        inputs.files(diagramSources)
        // 파일 인자 없이 실행하면 PlantUML이 다른 동작을 하므로, 렌더링할 파일이 없으면 건너뛴다.
        onlyIf { !diagramSources.isEmpty }
        argumentProviders.add(CommandLineArgumentProvider {
            listOf("-t$format", "-charset", "UTF-8", "-failfast2") + diagramSources.files.map { it.absolutePath }
        })
    }

val renderDiagramsPng = registerDiagramRender("renderDiagramsPng", "png")
val renderDiagramsSvg = registerDiagramRender("renderDiagramsSvg", "svg")

tasks.register("renderDiagrams") {
    group = "documentation"
    description = "Render src/**/docs/diagrams/*.puml to png and svg"
    dependsOn(renderDiagramsPng, renderDiagramsSvg)
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
