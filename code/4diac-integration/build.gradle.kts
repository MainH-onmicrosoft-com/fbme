import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    mps
}
    kotlin
}

sourceSets {
    test {
        java.srcDir("src/test/kotlin")
        resources {
            srcDir("src/test/resources")
        }
    }
}
dependencies {
    mpsImplementation(project(":code:library", "mps"))
    mpsImplementation(project(":code:language", "mps"))
    mpsImplementation(project(":code:platform", "mps"))

    compileOnly(project(":code:library"))

    testImplementation("io.mockk:mockk:1.13.9") {
        exclude(group="org.jetbrains.kotlin")
    }
    testRuntimeOnly("org.jetbrains.intellij.deps:jdom:2.0.6")
    testImplementation(kotlin("reflect"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testImplementation(project(":code:library"))
}

mps {
    buildScriptName.set("fbme_fordiac")
    moduleName.set("org.fbme.integration.fordiac.lib")
}

val compileKotlin by tasks.getting(KotlinCompile::class) {
    kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}