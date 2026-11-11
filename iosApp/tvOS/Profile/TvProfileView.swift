import SwiftUI
import Shared

/// Профиль Apple TV — как на Android TV: аккаунт, источник контента, настройки воспроизведения и
/// выход. Настройки и выход доступны и без загруженного профиля: сбой профиля не должен запирать
/// человека в источнике.
struct TvProfileView: View {
    @StateObject private var viewModel = ProfileViewModel()
    @State private var logoutAsked = false

    var body: some View {
        TvScreenScroll {
            TvProfileAccount(viewModel: viewModel)
            sourceSection
            TvPlaybackSettings(viewModel: viewModel)
            logoutButton
        }
        .task { await viewModel.load() }
        .task { await viewModel.observeSettings() }
        .confirmationDialog(
            "Выйти из «\(viewModel.source.label)»?",
            isPresented: $logoutAsked,
            titleVisibility: .visible
        ) {
            Button("Выйти", role: .destructive) { Task { await viewModel.logout() } }
            Button("Отмена", role: .cancel) {}
        } message: {
            Text("Вход в другой источник сохранится.")
        }
    }

    /// Смена источника пересоберёт разделы с начала, а без входа в новый источник — покажет вход.
    private var sourceSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            TvSectionHeader(title: "Источник")
                .padding(.horizontal, DS.tvSafeHorizontal)
            TvChipRow(
                options: viewModel.sourceOptions.map { TvChipOption(value: $0, title: $0.label) },
                selected: viewModel.source
            ) { source in
                Task { await viewModel.select(source) }
            }
        }
    }

    private var logoutButton: some View {
        Button(role: .destructive) {
            logoutAsked = true
        } label: {
            Label("Выйти", systemImage: "rectangle.portrait.and.arrow.right")
        }
        .tvActionStyle(.secondary)
        .padding(.horizontal, DS.tvSafeHorizontal)
        .padding(.top, DS.Spacing.lg)
    }
}

/// Аккаунт: инициалы на втором акценте (как аватар во всей дизайн-системе), имя и подпись —
/// срок подписки или название источника.
private struct TvProfileAccount: View {
    @ObservedObject var viewModel: ProfileViewModel

    var body: some View {
        HStack(spacing: DS.Spacing.xl) {
            avatar
            VStack(alignment: .leading, spacing: DS.Spacing.sm) {
                Text(viewModel.profile?.username ?? viewModel.source.label)
                    .font(TvType.screenTitle)
                    .foregroundColor(Theme.text)
                Text(viewModel.accountCaption)
                    .font(TvType.body)
                    .foregroundColor(Theme.textSecondary)
                if let error = viewModel.error {
                    Text(error)
                        .font(TvType.caption)
                        .foregroundColor(Theme.error)
                }
            }
            if viewModel.isLoading {
                ProgressView().tint(Theme.accent)
            }
        }
        .padding(.horizontal, DS.tvSafeHorizontal)
        .padding(.top, DS.Spacing.lg)
    }

    private var avatar: some View {
        ZStack {
            Circle().fill(Theme.accent2)
            if let profile = viewModel.profile, !profile.initials().isEmpty {
                Text(profile.initials())
                    .font(TvType.screenTitle)
                    .foregroundColor(Theme.background)
            } else {
                Image(systemName: "person.fill")
                    .font(TvType.icon)
                    .foregroundColor(Theme.background)
            }
        }
        .frame(width: TvMetrics.avatarSize, height: TvMetrics.avatarSize)
    }
}

/// Качество, озвучка и субтитры по умолчанию — их применяет плеер на старте.
private struct TvPlaybackSettings: View {
    @ObservedObject var viewModel: ProfileViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            TvSectionHeader(title: "Воспроизведение")
                .padding(.horizontal, DS.tvSafeHorizontal)
            settingRow("Качество", options: viewModel.qualityOptions, selected: viewModel.settings?.quality) { value in
                Task { await viewModel.setQuality(value) }
            }
            settingRow("Озвучка", options: viewModel.audioOptions, selected: viewModel.settings?.audioLanguage) { value in
                Task { await viewModel.setAudio(value) }
            }
            settingRow("Субтитры", options: viewModel.subtitleOptions, selected: viewModel.settings?.subtitleLanguage) { value in
                Task { await viewModel.setSubtitles(value) }
            }
        }
    }

    /// Пока настройки не пришли, ни один чип не отмечен — лучше, чем отметить догадку.
    private func settingRow(
        _ title: String,
        options: [String],
        selected: String?,
        onSelect: @escaping (String) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            TvFieldLabel(title: title).padding(.top, DS.Spacing.md)
            TvChipRow(
                options: options.map { TvChipOption<String?>(value: $0, title: $0) },
                selected: selected
            ) { value in
                if let value { onSelect(value) }
            }
        }
    }
}
