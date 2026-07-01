import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

// Единый статический анализ (detekt) + форматирование (detekt-formatting → ktlint, стиль official).
// Подключается во всех модулях через convention-плагины (см. filmax.android.* / filmax.kmp.library).
plugins {
    id("io.gitlab.arturbosch.detekt")
}

extensions.configure<DetektExtension> {
    // Берём дефолтный набор правил detekt и накатываем поверх наш detekt.yml.
    buildUponDefaultConfig = true
    parallel = true
    // Общий конфиг в корне репозитория — единые правила для всех модулей.
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    // Бейзлайн — per-module: существующий код заносится в baseline, новый — проверяется.
    baseline = file("detekt-baseline.xml")
    // Покрываем JVM-, Android- и KMP-раскладки исходников. Несуществующие пути detekt пропускает.
    source.setFrom(
        "src/main/kotlin",
        "src/main/java",
        "src/test/kotlin",
        "src/commonMain/kotlin",
        "src/androidMain/kotlin",
        "src/appleMain/kotlin",
        "src/iosMain/kotlin",
        "src/commonTest/kotlin",
    )
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "detektPlugins"(versionCatalog.findLibrary("detekt-formatting").get())
    // Кастомные правила Filmax (напр. NestedIf). Guard от самоссылки — сам модуль
    // правил detekt не анализирует и в detektPlugins себя не тянет.
    if (path != ":detekt-rules") {
        "detektPlugins"(project(":detekt-rules"))
    }
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    // Тот же конфиг и при генерации baseline.
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}
