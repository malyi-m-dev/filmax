import SwiftUI
import Shared

/// Профиль для iPhone/iPad: аккаунт, подписка, реальные настройки воспроизведения, выход.
struct ProfileView: View {
    @StateObject private var viewModel = ProfileViewModel()

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.profile == nil {
                LoadingView()
            } else if let error = viewModel.error, viewModel.profile == nil {
                ErrorView(message: error) { Task { await viewModel.load() } }
            } else {
                content
            }
        }
        .navigationTitle("Профиль")
        .background(Theme.background.ignoresSafeArea())
        .task { if viewModel.profile == nil { await viewModel.load() } }
        .task { await viewModel.observeSettings() }
    }

    private var content: some View {
        ScrollView {
            VStack(spacing: DS.Spacing.xl) {
                if let profile = viewModel.profile { header(profile) }
                if let subscription = viewModel.profile?.subscription { subscriptionCard(subscription) }
                playbackSettings
                FilmaxButton(title: "Выйти", systemImage: "rectangle.portrait.and.arrow.right", style: .secondary) {
                    viewModel.signOut()
                }
                .padding(.horizontal, DS.Spacing.md)
            }
            .padding(.vertical, DS.Spacing.lg)
        }
    }

    private func header(_ profile: UserProfile) -> some View {
        VStack(spacing: DS.Spacing.md) {
            avatar(profile)
            Text(profile.username)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(.white)
            if let email = profile.email, !email.isEmpty {
                Text(email)
                    .font(.system(size: 14))
                    .foregroundColor(Theme.onSurfaceVariant)
            }
        }
    }

    @ViewBuilder private func avatar(_ profile: UserProfile) -> some View {
        if let url = profile.avatarUrl, !url.isEmpty {
            AsyncImage(url: URL(string: url)) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                initialsCircle(profile)
            }
            .frame(width: 92, height: 92)
            .clipShape(Circle())
        } else {
            initialsCircle(profile)
        }
    }

    private func initialsCircle(_ profile: UserProfile) -> some View {
        Text(profile.initials())
            .font(.system(size: 34, weight: .bold))
            .foregroundColor(.white)
            .frame(width: 92, height: 92)
            .background(Theme.accent)
            .clipShape(Circle())
    }

    private func subscriptionCard(_ subscription: FilmSubscription) -> some View {
        HStack(spacing: DS.Spacing.md) {
            Image(systemName: subscription.active ? "crown.fill" : "crown")
                .font(.system(size: 24))
                .foregroundColor(Theme.accent)
            VStack(alignment: .leading, spacing: 2) {
                Text("Подписка")
                    .font(.system(size: 13))
                    .foregroundColor(Theme.onSurfaceVariant)
                Text(subscription.active ? "Активна" : "Неактивна")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.white)
            }
            Spacer()
        }
        .padding(DS.Spacing.md)
        .background(Theme.surface)
        .clipShape(RoundedRectangle(cornerRadius: DS.Radius.md))
        .padding(.horizontal, DS.Spacing.md)
    }

    private var playbackSettings: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            SectionHeader(title: "Воспроизведение").padding(.horizontal, DS.Spacing.md)
            VStack(spacing: 0) {
                settingRow("Качество видео", value: viewModel.settings?.quality, options: viewModel.qualityOptions) {
                    Task { await viewModel.setQuality($0) }
                }
                Divider().overlay(Theme.onSurfaceVariant.opacity(0.2))
                settingRow("Язык аудио", value: viewModel.settings?.audioLanguage, options: viewModel.audioOptions) {
                    Task { await viewModel.setAudio($0) }
                }
                Divider().overlay(Theme.onSurfaceVariant.opacity(0.2))
                settingRow("Субтитры", value: viewModel.settings?.subtitleLanguage, options: viewModel.subtitleOptions) {
                    Task { await viewModel.setSubtitle($0) }
                }
            }
            .background(Theme.surface)
            .clipShape(RoundedRectangle(cornerRadius: DS.Radius.md))
            .padding(.horizontal, DS.Spacing.md)
        }
    }

    private func settingRow(_ title: String, value: String?, options: [String], onSelect: @escaping (String) -> Void) -> some View {
        Menu {
            ForEach(options, id: \.self) { option in
                Button {
                    onSelect(option)
                } label: {
                    if option == value { Label(option, systemImage: "checkmark") } else { Text(option) }
                }
            }
        } label: {
            HStack {
                Text(title).font(.system(size: 15)).foregroundColor(.white)
                Spacer()
                Text(value ?? "—").font(.system(size: 15)).foregroundColor(Theme.onSurfaceVariant)
                Image(systemName: "chevron.up.chevron.down").font(.system(size: 12)).foregroundColor(Theme.onSurfaceVariant)
            }
            .padding(DS.Spacing.md)
        }
    }
}
