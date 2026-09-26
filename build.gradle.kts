// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.gms) apply false
    // Declared here (classpath only) so subprojects can apply it below. apply false = not applied to root.
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.cyclonedx) apply false
}

// Headless Kotlin static analysis applied to EVERY module (:app, :common, :deezer-extension). This is the
// ONLY static analyzer that covers the Kotlin/JVM :deezer-extension and the KMP :common — neither has an
// Android Lint task. Left at Detekt's DEFAULT config: PSI-only (no type resolution), which is fast and
// structurally immune to the IDE "Inspect Code" reified-generic/@OptIn type-inference hang (detekt is a
// batch parser, not the IDE's incremental inference engine). Run: `./gradlew detekt` (all modules) or
// `./gradlew :deezer-extension:detekt` (one). Reports: <module>/build/reports/detekt/detekt.{html,xml}.
// If Gradle's configuration cache / project-isolation ever rejects this cross-project block, the equivalent
// fallback is to add `alias(libs.plugins.detekt)` to each module's own plugins{} block instead.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    // Coverage (kover) + SBOM for the dependency scan. Both are report-only: neither can fail a build,
    // they only produce the numbers CONSTRAINTS.md checks against.
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "org.cyclonedx.bom")
    // Scope the SBOM to shipped runtime only. Default scans EVERY resolvable configuration (buildscript,
    // tooling, test classpaths): first run produced 105 vulns including freemarker/jackson/netty/bcprov,
    // none of which is in any release runtime classpath. Names below are full-match regexes; exclusion wins.
    tasks.withType<org.cyclonedx.gradle.CyclonedxDirectTask>().configureEach {
        includeConfigs.set(
            listOf(
                "releaseRuntimeClasspath", // :app (AGP)
                "androidRuntimeClasspath", // :common android target (KMP)
                "jvmRuntimeClasspath", // :common jvm target (KMP)
                "runtimeClasspath" // :deezer-extension (plain JVM)
            )
        )
        skipConfigs.set(listOf("(?i).*test.*"))
    }
}
