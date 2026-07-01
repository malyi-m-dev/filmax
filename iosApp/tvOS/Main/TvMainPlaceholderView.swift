import SwiftUI

/// Заглушка авторизованного состояния на Apple TV. `MainViewModel` — общий (Shared/).
struct TvMainPlaceholderView: View {
    @StateObject private var viewModel = MainViewModel()

    var body: some View {
        VStack(spacing: 32) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 120))
                .foregroundColor(Theme.accent)
            Text("Вы вошли в Filmax")
                .font(.system(size: 44, weight: .bold))
                .foregroundColor(.white)
            Text("Главный экран Apple TV — следующий срез. Авторизация уже работает на общей логике KMP.")
                .font(.system(size: 26))
                .foregroundColor(Theme.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 900)
            Button("Выйти") { viewModel.signOut() }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
        }
        .padding(60)
    }
}
