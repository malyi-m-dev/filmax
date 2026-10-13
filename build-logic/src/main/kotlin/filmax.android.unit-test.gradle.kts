// Юнит-тесты Android-модулей: kotlin.test поверх JUnit 4 (раннер AGP по умолчанию — платформу
// настраивать не нужно) и coroutines-test для ScreenModel'ей. Подключается из
// filmax.android.library и filmax.android.compose, поэтому тест кладётся в src/test любого модуля
// без правки его build.gradle.kts.

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "testImplementation"(libs.findLibrary("kotlin-test-junit").get())
    "testImplementation"(libs.findLibrary("kotlinx-coroutines-test").get())
}
