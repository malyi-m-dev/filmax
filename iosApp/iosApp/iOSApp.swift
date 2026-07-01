import SwiftUI
import Shared

// Точка входа iOS-приложения Filmax.
// Бизнес-логика (data/domain) — общая из KMP-фреймворка `Shared`; презентейшен целиком на Swift.
@main
struct iOSApp: App {
    init() {
        // Инициализация Koin из общего кода (те же модули, что и на Android).
        KoinKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .preferredColorScheme(.dark)
        }
    }
}
