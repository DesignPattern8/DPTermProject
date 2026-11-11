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
// src/**/docs/diagrams/*.puml 을 같은 폴더에 png·svg로 렌더링한다.
//
//   ./gradlew renderDiagrams -Pdir=src/com/holub/life/twoplayer   # 이 폴더 아래만 (권장)
//   ./gradlew renderDiagrams                                       # 전체. 경고를 낸다
//
// -Pdir는 이 프로젝트(Codes/GameOfLife) 기준 경로이고, src 안이어야 한다. docs/diagrams 폴더 자체를 줘도 된다.
// 없는 폴더, src 밖의 폴더, .puml이 없는 폴더를 주면 실패한다.
// -Pdir 없이 전체를 렌더링하면 다른 도메인의 png·svg도 다시 만들어진다. 그 파일을 만든 환경과
// PlantUML 버전이나 레이아웃 엔진(Graphviz 유무)이 다르면 .puml이 같아도 결과가 달라지므로(이슈 #39),
// 내 도메인만 렌더링하고 다른 도메인의 결과 파일은 커밋하지 않는다.
// 리팩토링 전 원본 다이어그램(저장소 루트의 diagrams/)은 보존 대상이라 렌더링하지 않는다.
// Graphviz 없이도 상태·클래스·시퀀스 다이어그램이 렌더링되는 것을 확인했다(이슈 #22). 레이아웃이 깨지면 .puml에 `!pragma layout smetana`를 넣는다.
// 결과 파일 이름은 `@startuml <이름>`을 따르므로 이름을 파일명과 같게 쓴다.
val plantuml: Configuration = configurations.create("plantuml")

dependencies {
    plantuml("net.sourceforge.plantuml:plantuml:1.2026.9")
}

val sourceRoot: File = file("src")
val diagramDir: String? = providers.gradleProperty("dir").orNull

// -Pdir가 주어지면 그 폴더를, 아니면 src 전체를 기준으로 docs/diagrams/*.puml 을 찾는다.
val diagramRoot: File = diagramDir?.let { file(it) } ?: sourceRoot
val diagramSources = fileTree(diagramRoot) {
    include("**/*.puml")
}.filter { it.parentFile.name == "diagrams" && it.parentFile.parentFile?.name == "docs" }

val checkDiagramScope = tasks.register("checkDiagramScope") {
    group = "documentation"
    description = "Validate -Pdir for renderDiagrams and report what will be rendered"
    // configuration cache에 담을 수 있도록 스크립트 변수 대신 지역 값을 쓴다.
    val dir = diagramDir
    val root = diagramRoot
    val src = sourceRoot
    val sources = diagramSources
    val base = projectDir
    doLast {
        if (dir != null) {
            val rootDir = root.canonicalFile
            val srcDir = src.canonicalFile
            if (!rootDir.isDirectory) {
                throw GradleException("-Pdir=$dir: 폴더가 없습니다 ($rootDir)")
            }
            if (!rootDir.startsWith(srcDir)) {
                throw GradleException("-Pdir=$dir: src 안의 폴더만 렌더링합니다. 루트 diagrams/는 보존 대상입니다 ($rootDir)")
            }
            if (sources.isEmpty) {
                throw GradleException("-Pdir=$dir: 이 폴더 아래에 docs/diagrams/*.puml 이 없습니다")
            }
        } else if (!sources.isEmpty) {
            val folders = sources.files
                .map { it.parentFile.relativeTo(base).invariantSeparatorsPath }
                .toSortedSet()
            logger.warn(
                "renderDiagrams: -Pdir 없이 전체를 렌더링합니다. 아래 폴더의 png·svg가 모두 다시 만들어집니다.\n" +
                    folders.joinToString("\n") { "  - $it" } + "\n" +
                    "  다른 도메인의 결과 파일은 만든 환경(PlantUML 버전, Graphviz 유무)이 다르면 내용이 바뀝니다.\n" +
                    "  내 도메인만 렌더링하려면: ./gradlew renderDiagrams -Pdir=<폴더>"
            )
        }
        sources.files.sorted().forEach {
            logger.lifecycle("renderDiagrams: ${it.relativeTo(base).invariantSeparatorsPath}")
        }
    }
}

fun registerDiagramRender(name: String, format: String, sources: FileCollection, tool: FileCollection) =
    tasks.register<JavaExec>(name) {
        group = "documentation"
        description = "Render docs/diagrams/*.puml under -Pdir (default: src) to $format next to each source"
        dependsOn(checkDiagramScope)
        classpath = tool
        mainClass.set("net.sourceforge.plantuml.Run")
        jvmArgs("-Djava.awt.headless=true")
        inputs.files(sources)
        // 파일 인자 없이 실행하면 PlantUML이 다른 동작을 하므로, 렌더링할 파일이 없으면 건너뛴다.
        onlyIf { !sources.isEmpty }
        argumentProviders.add(CommandLineArgumentProvider {
            listOf("-t$format", "-charset", "UTF-8", "-failfast2") + sources.files.map { it.absolutePath }
        })
    }

val renderDiagramsPng = registerDiagramRender("renderDiagramsPng", "png", diagramSources, plantuml)
val renderDiagramsSvg = registerDiagramRender("renderDiagramsSvg", "svg", diagramSources, plantuml)

tasks.register("renderDiagrams") {
    group = "documentation"
    description = "Render docs/diagrams/*.puml to png and svg (-Pdir=<folder under src> to limit the scope)"
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
