import SwiftUI
import Shared

/// Профиль для Apple TV: аккаунт, подписка, реальные настройки воспроизведения (focus), выход.
/// Только реальные настройки — без заглушек (как TV-Профиль на Android).
struct TvProfileView: View {
    @StateObject private var viewModel = ProfileViewModel()

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.profile == nil {
                TvLoadingView()
            } else if let error = viewModel.error, viewModel.profile == nil {
                TvErrorView(message: error) { Task { await viewModel.load() } }
            } else {
                content
            }
        }
        .background(Theme.background.ignoresSafeArea())
        .task { if viewModel.profile == nil { await viewModel.load() } }
        .task { await viewModel.observeSettings() }
    }

    private var content: some View {
        ScrollView {
            HStack(alignment: .top, spacing: DS.Spacing.xxl) {
                if let profile = viewModel.profile { account(profile) }
                VStack(alignment: .leading, spacing: DS.Spacing.xl) {
                    playbackSettings
                    Button {
                        viewModel.signOut()
                    } label: {
                        Label("Выйти", systemImage: "rectangle.portrait.and.arrow.right")
                            .padding(.horizontal, DS.Spacing.lg)
                    }
                    .buttonStyle(.borderedProminent).tint(Theme.accent)
                }
            }
            .padding(.horizontal, DS.tvSafeHorizontal)
            .padding(.vertical, DS.tvSafeVertical)
        }
    }

    private func account(_ profile: UserProfile) -> some View {
        VStack(spacing: DS.Spacing.lg) {
            Text(profile.initials())
                .font(.system(size: 60, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 180, height: 180)
                .background(Theme.accent)
                .clipShape(Circle())
            Text(profile.username)
                .font(.system(size: 34, weight: .bold))
                .foregroundColor(.white)
            if let email = profile.email, !email.isEmpty {
                Text(email).font(.system(size: 22)).foregroundColor(Theme.onSurfaceVariant)
            }
            if let subscription = profile.subscription {
                Label(subscription.active ? "Подписка активна" : "Подписка неактивна",
                      systemImage: subscription.active ? "crown.fill" : "crown")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundColor(Theme.accent)
            }
        }
        .frame(width: 460)
    }

    private var playbackSettings: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.lg) {
            TvSectionHeader(title: "Воспроизведение")
            settingGroup("Качество видео", value: viewModel.settings?.quality, options: viewModel.qualityOptions) {
                Task { await viewModel.setQuality($0) }
            }
            settingGroup("Язык аудио", value: viewModel.settings?.audioLanguage, options: viewModel.audioOptions) {
                Task { await viewModel.setAudio($0) }
            }
            settingGroup("Субтитры", value: viewModel.settings?.subtitleLanguage, options: viewModel.subtitleOptions) {
                Task { await viewModel.setSubtitle($0) }
            }
        }
    }

    private func settingGroup(_ title: String, value: String?, options: [String], onSelect: @escaping (String) -> Void) -> some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            Text(title).font(.system(size: 24, weight: .semibold)).foregroundColor(Theme.onSurfaceVariant)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.md) {
                    ForEach(options, id: \.self) { option in
                        Button {
                            onSelect(option)
                        } label: {
                            Text(option).padding(.horizontal, DS.Spacing.md)
                        }
                        .buttonStyle(.bordered)
                        .tint(option == value ? Theme.accent : Theme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
