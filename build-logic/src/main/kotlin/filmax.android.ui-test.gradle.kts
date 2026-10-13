// UI-тесты Compose-модулей на JVM: Robolectric + ui-test-junit4, без эмулятора — поэтому их
// гоняет CI вместе с юнит-тестами (testDebugUnitTest). Подключается из filmax.android.compose:
// UI-тест кладётся в src/test любого UI-модуля без правки его build.gradle.kts.

import com.android.build.gradle.LibraryExtension

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<LibraryExtension> {
    // Robolectric поднимает Activity и ресурсы — без этого тема и строки в тесте пустые.
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    val composeBom = platform(libs.findLibrary("compose-bom").get())
    "testImplementation"(composeBom)
    "testImplementation"(libs.findLibrary("compose-ui-test-junit4").get())
    "testImplementation"(libs.findLibrary("robolectric").get())
    // ComponentActivity для createComposeRule — из манифеста debug-варианта.
    "debugImplementation"(composeBom)
    "debugImplementation"(libs.findLibrary("compose-ui-test-manifest").get())
}
