import SwiftUI

/// Асинхронная загрузка постера/бэкдропа из URL-строки общего слоя с плейсхолдером.
/// Живёт в `Shared/` — переиспользуется компонентами iOS и tvOS.
struct PosterImage: View {
    let url: String?
    var placeholderIcon: String = "film"

    var body: some View {
        AsyncImage(url: URL(string: url ?? "")) { phase in
            switch phase {
            case .success(let image):
                image.resizable()
            default:
                ZStack {
                    Theme.surface
                    Image(systemName: placeholderIcon)
                        .font(.system(size: 28))
                        .foregroundColor(Theme.onSurfaceVariant.opacity(0.6))
                }
            }
        }
    }
}
