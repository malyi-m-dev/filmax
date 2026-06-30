import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Кастомные detekt-правила Filmax. Чистый JVM-модуль: намеренно НЕ применяет filmax.detekt,
// иначе возникла бы циклическая зависимость через detektPlugins(project(":detekt-rules")).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.detekt.api)
    testImplementation(libs.detekt.test)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
