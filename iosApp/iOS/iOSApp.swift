import SwiftUI
import UIKit
import Shared

/// Точка входа iPhone/iPad. Data/domain — общие из KMP-фреймворка `Shared`, презентейшен — SwiftUI.
@main
struct iOSApp: App {
    init() {
        // Версию приложения и устройство знает только приложение, а нужны они домашнему серверу
        // (заголовок клиента Jellyfin). Дефолтные параметры Kotlin в Swift не видны — передаём явно.
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
        let client = ClientInfo(
            appVersion: version,
            isTv: false,
            device: UIDevice.current.model,
            os: "iOS \(UIDevice.current.systemVersion)"
        )
        KoinKt.doInitKoin(client: client, appDeclaration: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .preferredColorScheme(.dark)
        }
    }
}
