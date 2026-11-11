import SwiftUI
import UIKit
import Shared

/// Точка входа Apple TV. Data/domain — общие из KMP-фреймворка `Shared`, ViewModel'и — общие с iOS
/// (`Shared/`); отличается только SwiftUI-презентейшен под пульт и focus engine.
@main
struct FilmaxTVApp: App {
    init() {
        // Версию приложения и устройство знает только приложение, а нужны они домашнему серверу:
        // заголовок клиента Jellyfin и профиль кодеков ТВ. Дефолтные параметры Kotlin в Swift
        // не видны — декларация Koin передаётся явно, пустой.
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
        let client = ClientInfo(
            appVersion: version,
            isTv: true,
            device: "Apple TV",
            os: "tvOS \(UIDevice.current.systemVersion)"
        )
        KoinKt.doInitKoin(client: client, appDeclaration: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            TvRootView()
                .preferredColorScheme(.dark)
        }
    }
}
