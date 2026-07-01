import SwiftUI
import Shared

// Точка входа приложения Apple TV (tvOS). Общий data/domain — из KMP `Shared`,
// те же ViewModel'и, что и на iOS; отличается только SwiftUI-презентейшен (focus-навигация).
@main
struct FilmaxTVApp: App {
    init() {
        KoinKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            TvRootView()
                .preferredColorScheme(.dark)
        }
    }
}
