import SwiftUI

/// Заглушка авторизованного состояния (iPhone/iPad). `MainViewModel` — общий (Shared/).
struct MainPlaceholderView: View {
    @StateObject private var viewModel = MainViewModel()

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 64))
                .foregroundColor(Theme.accent)
            Text("Вы вошли в Filmax")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(.white)
            Text("Главный экран — следующий срез. Онбординг и авторизация уже работают на общей логике KMP.")
                .font(.system(size: 15))
                .foregroundColor(Theme.onSurfaceVariant)
                .multilineTextAlignment(.center)
            Button("Выйти") { viewModel.signOut() }
                .buttonStyle(.bordered)
                .tint(Theme.accent)
        }
        .padding(28)
    }
}
