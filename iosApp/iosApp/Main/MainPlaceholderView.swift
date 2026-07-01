import SwiftUI
import Shared

@MainActor
final class MainViewModel: ObservableObject {
    private let logout = UseCaseProvider.shared.logoutUseCase()

    func signOut() {
        // Очищает токены общим data-слоем; поток авторизации вернёт RootView на онбординг.
        Task { _ = try? await logout.invoke() }
    }
}

/// Заглушка авторизованного состояния — следующий вертикальный срез (Главная и т.д.).
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
