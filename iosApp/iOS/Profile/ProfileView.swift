import SwiftUI
import Shared

/// Профиль iPhone/iPad — нативная форма: аккаунт, источник, воспроизведение, выход. Подписка —
/// только у источника, у которого она есть; у домашнего сервера в карточке — сам сервер.
struct ProfileView: View {
    @StateObject private var viewModel = ProfileViewModel()
    @State private var confirmLogout = false

    var body: some View {
        Form {
            accountSection
            Section("Источник") {
                Picker("Откуда смотреть", selection: Binding(
                    get: { viewModel.source },
                    set: { next in Task { await viewModel.select(next) } }
                )) {
                    ForEach(viewModel.sourceOptions, id: \.self) { source in Text(source.label).tag(source) }
                }
            }
            if let settings = viewModel.settings { playbackSection(settings) }
            Section {
                Button("Выйти из аккаунта", role: .destructive) { confirmLogout = true }
            } footer: {
                Text("Выход — только из текущего источника: вход в другой сохранится.")
            }
        }
        .scrollContentBackground(.hidden)
        .background(Theme.background.ignoresSafeArea())
        .navigationTitle("Профиль")
        .confirmationDialog("Выйти из \(viewModel.source.label)?", isPresented: $confirmLogout, titleVisibility: .visible) {
            Button("Выйти", role: .destructive) { Task { await viewModel.logout() } }
        }
        .task { await viewModel.load() }
        .task { await viewModel.observeSettings() }
    }

    private var accountSection: some View {
        Section {
            HStack(spacing: DS.Spacing.md) {
                PosterImage(url: viewModel.profile?.avatarUrl, placeholderIcon: "person.fill")
                    .frame(width: 52, height: 52)
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(viewModel.profile?.username ?? "…").font(.headline)
                    Text(viewModel.accountCaption).font(.footnote).foregroundColor(Theme.textSecondary)
                }
            }
            .padding(.vertical, DS.Spacing.xs)
            if let error = viewModel.error { Text(error).font(.footnote).foregroundColor(Theme.error) }
        }
    }

    private func playbackSection(_ settings: PlaybackSettings) -> some View {
        Section("Воспроизведение") {
            optionPicker("Качество", options: viewModel.qualityOptions, current: settings.quality) { value in
                await viewModel.setQuality(value)
            }
            optionPicker("Озвучка", options: viewModel.audioOptions, current: settings.audioLanguage) { value in
                await viewModel.setAudio(value)
            }
            optionPicker("Субтитры", options: viewModel.subtitleOptions, current: settings.subtitleLanguage) { value in
                await viewModel.setSubtitles(value)
            }
        }
    }

    private func optionPicker(
        _ title: String,
        options: [String],
        current: String,
        apply: @escaping (String) async -> Void
    ) -> some View {
        Picker(title, selection: Binding(get: { current }, set: { value in Task { await apply(value) } })) {
            ForEach(options, id: \.self) { option in Text(option).tag(option) }
        }
    }
}
