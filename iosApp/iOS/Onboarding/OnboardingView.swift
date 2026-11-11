import SwiftUI

/// Вход iPhone/iPad: сначала откуда смотреть — kino.pub или свой домашний сервер, — потом вход в
/// выбранный источник. Как на Android: вход в каждый источник свой и переживает переключение.
struct OnboardingView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: DS.Spacing.lg) {
                Spacer()
                Image(systemName: "film.stack")
                    .font(.system(size: 56))
                    .foregroundStyle(Theme.accent)
                Text("Filmax")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(Theme.text)
                Text("Фильмы и сериалы с kino.pub или с вашего домашнего сервера Jellyfin.")
                    .font(.callout)
                    .foregroundColor(Theme.textSecondary)
                    .multilineTextAlignment(.center)
                Spacer()
                NavigationLink {
                    KinopubLoginView()
                } label: {
                    sourceLabel(title: "kino.pub", detail: "Вход по коду на kino.pub/device", systemImage: "play.tv")
                }
                NavigationLink {
                    HomeServerLoginView()
                } label: {
                    sourceLabel(title: "Домашний сервер", detail: "Jellyfin в вашей сети, без подписки", systemImage: "server.rack")
                }
            }
            .padding(DS.Spacing.lg)
            .background(Theme.background.ignoresSafeArea())
        }
    }

    private func sourceLabel(title: String, detail: String, systemImage: String) -> some View {
        HStack(spacing: DS.Spacing.md) {
            Image(systemName: systemImage).font(.title2).frame(width: 36)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline)
                Text(detail).font(.footnote).foregroundColor(Theme.textSecondary)
            }
            Spacer()
            Image(systemName: "chevron.right").foregroundColor(Theme.onSurfaceVariant)
        }
        .foregroundColor(Theme.text)
        .padding(DS.Spacing.md)
        .background(Theme.surface)
        .clipShape(RoundedRectangle(cornerRadius: DS.Radius.md))
    }
}

/// Вход в kino.pub по коду устройства: код вводят на kino.pub/device с любого устройства, а
/// приложение ждёт подтверждения — пароль здесь не нужен.
struct KinopubLoginView: View {
    @StateObject private var viewModel = OnboardingViewModel()

    var body: some View {
        VStack(spacing: DS.Spacing.lg) {
            Text("Откройте на телефоне или компьютере")
                .foregroundColor(Theme.textSecondary)
            Text(viewModel.verificationUri ?? "kino.pub/device")
                .font(.headline)
                .foregroundColor(Theme.accent)
            if let code = viewModel.userCode {
                Text(code)
                    .font(.system(size: 44, weight: .bold, design: .monospaced))
                    .foregroundColor(Theme.text)
                    .textSelection(.enabled)
                Text("и введите этот код").foregroundColor(Theme.textSecondary)
                if viewModel.polling { ProgressView().tint(Theme.accent) }
            } else if let error = viewModel.error {
                ErrorView(message: error) { viewModel.retry() }
            } else {
                ProgressView().tint(Theme.accent)
            }
        }
        .padding(DS.Spacing.lg)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Theme.background.ignoresSafeArea())
        .navigationTitle("kino.pub")
        .task { await viewModel.start() }
        .onDisappear { viewModel.cancel() }
    }
}
