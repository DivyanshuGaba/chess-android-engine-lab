plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

// The chess rules and both built-in engines are plain Kotlin, so this module compiles the app's own
// domain sources directly instead of keeping a copy: whatever the benchmarks measure is the code the app runs.
val domain = "../app/src/main/java/com/divyanshu/chess/domain"

kotlin {
    jvmToolchain(17)
    sourceSets.main {
        kotlin.srcDir("$domain/model")
        kotlin.srcDir("$domain/engine")
        kotlin.srcDir("$domain/ai")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}

application {
    mainClass.set("com.divyanshu.chess.uci.MainKt")
    applicationDefaultJvmArgs = listOf("-XX:+UseSerialGC", "-Xss16m", "-Xms64m", "-XX:TieredStopAtLevel=4")
}
