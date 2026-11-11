import SwiftUI
import Shared

/// Детали iPhone/iPad: кадр, оценки с подписью источника, «Смотреть», «Буду смотреть», «В папку»
/// (только у источника с папками), сезоны и серии, состав, «Похожие».
struct DetailsView: View {
    @StateObject private var viewModel: DetailsViewModel
    @State private var foldersOpen = false

    init(itemId: Int32) {
        _viewModel = StateObject(wrappedValue: DetailsViewModel(itemId: itemId))
    }

    var body: some View {
        Group {
            if let item = viewModel.item {
                content(item)
            } else if let error = viewModel.error {
                ErrorView(message: error) { Task { await viewModel.load() } }
            } else {
                LoadingView()
            }
        }
        .background(Theme.background.ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $foldersOpen) { FolderPickerSheet(viewModel: viewModel) }
        .task { if viewModel.item == nil { await viewModel.load() } }
        .task { await viewModel.observeWatchlist() }
    }

    private func content(_ item: Item) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DS.Spacing.lg) {
                DetailsHeader(item: item)
                actions(item)
                if !item.plot.isEmpty {
                    Text(item.plot).font(.callout).foregroundColor(Theme.textSecondary).padding(.horizontal, DS.Spacing.md)
                }
                if item.isEpisodic, !item.seasonNumbers.isEmpty { EpisodesSection(item: item, viewModel: viewModel) }
                PeopleSection(item: item)
                if !viewModel.similar.isEmpty { ItemRail(title: "Похожие", items: viewModel.similar) }
            }
            .padding(.bottom, DS.Spacing.xl)
        }
        .ignoresSafeArea(edges: .top)
    }

    private func actions(_ item: Item) -> some View {
        VStack(spacing: DS.Spacing.sm) {
            NavigationLink(value: AppRoute.player(itemId: item.id, target: viewModel.playTarget)) {
                Label(viewModel.playTitle, systemImage: "play.fill").frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
            HStack(spacing: DS.Spacing.sm) {
                Button {
                    Task { await viewModel.toggleWatchlist() }
                } label: {
                    Label(viewModel.inWatchlist ? "В списке" : "Буду смотреть",
                          systemImage: viewModel.inWatchlist ? "bookmark.fill" : "bookmark")
                        .frame(maxWidth: .infinity)
                }
                // Папок у домашнего сервера нет — кнопки тоже нет, а не ошибка после нажатия.
                if viewModel.foldersAvailable {
                    Button { foldersOpen = true } label: {
                        Label("В папку", systemImage: viewModel.itemFolders.isEmpty ? "folder.badge.plus" : "folder.fill")
                            .frame(maxWidth: .infinity)
                    }
                }
            }
            .buttonStyle(.bordered)
            .tint(Theme.text)
        }
        .controlSize(.large)
        .padding(.horizontal, DS.Spacing.md)
    }
}

/// Кадр, название, мета и оценки: каждая подписана источником — «КП 8.1», «Рейтинг 7.5».
private struct DetailsHeader: View {
    let item: Item

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            PosterImage(url: item.backdropURL)
                .aspectRatio(16 / 10, contentMode: .fill)
                .frame(maxWidth: .infinity)
                .clipped()
                .overlay(LinearGradient(colors: [.clear, Theme.background], startPoint: .center, endPoint: .bottom))
            VStack(alignment: .leading, spacing: DS.Spacing.xs) {
                Text(item.type.label.uppercased()).font(.caption.weight(.bold)).foregroundColor(Theme.accent)
                Text(item.title).font(.system(size: 28, weight: .bold)).foregroundColor(Theme.text)
                Text([item.year > 0 ? "\(item.year)" : nil, item.country.isEmpty ? nil : item.country, item.durationText]
                    .compactMap { $0 }.joined(separator: " · "))
                    .font(.subheadline).foregroundColor(Theme.textSecondary)
                if !item.ratingLines.isEmpty {
                    Text(item.ratingLines.joined(separator: "   ")).font(.subheadline.weight(.semibold)).foregroundColor(Theme.accent2)
                }
            }
            .padding(DS.Spacing.md)
        }
    }
}

/// Сезоны чипами и серии выбранного сезона; серия открывается в плеере по своему адресу.
private struct EpisodesSection: View {
    let item: Item
    @ObservedObject var viewModel: DetailsViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            SectionHeader(title: "Серии").padding(.horizontal, DS.Spacing.md)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.sm) {
                    ForEach(item.seasonNumbers, id: \.self) { season in
                        FilterChip(title: "Сезон \(season)", selected: viewModel.selectedSeason == season) {
                            viewModel.selectedSeason = season
                        }
                    }
                }
                .padding(.horizontal, DS.Spacing.md)
            }
            LazyVStack(spacing: DS.Spacing.sm) {
                ForEach(viewModel.seasonEpisodes, id: \.id) { episode in
                    NavigationLink(value: AppRoute.player(itemId: item.id, target: episode.target)) {
                        EpisodeRow(episode: episode)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, DS.Spacing.md)
        }
    }
}

private struct EpisodeRow: View {
    let episode: MediaTrack

    var body: some View {
        HStack(spacing: DS.Spacing.md) {
            PosterImage(url: episode.thumbnail, placeholderIcon: "play.rectangle")
                .aspectRatio(16 / 9, contentMode: .fill)
                .frame(width: 120, height: 68)
                .clipShape(RoundedRectangle(cornerRadius: DS.Radius.sm))
            VStack(alignment: .leading, spacing: DS.Spacing.xs) {
                Text(episode.episodeCode).font(.caption.weight(.bold)).foregroundColor(Theme.accent)
                Text(episode.title.isEmpty ? "Серия \(episode.number)" : episode.title)
                    .font(.subheadline).foregroundColor(Theme.text).lineLimit(2)
                if let fraction = episode.progressFraction { ProgressView(value: fraction).tint(Theme.accent) }
            }
            Spacer()
            if episode.watchStatus == 1 { Image(systemName: "checkmark.circle.fill").foregroundColor(Theme.accent) }
        }
    }
}

/// Режиссёр и актёры — ссылками на фильмографию.
private struct PeopleSection: View {
    let item: Item

    private var cast: [String] {
        item.cast.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }

    var body: some View {
        if !cast.isEmpty || !item.director.isEmpty {
            VStack(alignment: .leading, spacing: DS.Spacing.sm) {
                SectionHeader(title: "В ролях").padding(.horizontal, DS.Spacing.md)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: DS.Spacing.sm) {
                        if !item.director.isEmpty {
                            NavigationLink(value: AppRoute.filmography(name: item.director, isDirector: true)) {
                                personChip(item.director, role: "Режиссёр")
                            }
                        }
                        ForEach(cast.prefix(15), id: \.self) { name in
                            NavigationLink(value: AppRoute.filmography(name: name, isDirector: false)) { personChip(name, role: nil) }
                        }
                    }
                    .padding(.horizontal, DS.Spacing.md)
                }
            }
        }
    }

    private func personChip(_ name: String, role: String?) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(name).font(.subheadline.weight(.semibold)).foregroundColor(Theme.text)
            if let role { Text(role).font(.caption2).foregroundColor(Theme.textSecondary) }
        }
        .padding(.horizontal, DS.Spacing.md)
        .padding(.vertical, DS.Spacing.sm)
        .background(Theme.surface)
        .clipShape(Capsule())
    }
}

/// Выбор папки: отметки — где тайтл уже лежит; новая папка сразу получает тайтл.
private struct FolderPickerSheet: View {
    @ObservedObject var viewModel: DetailsViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var newTitle = ""

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(viewModel.folders, id: \.id) { folder in
                        Button {
                            Task { await viewModel.toggle(folder: folder) }
                        } label: {
                            HStack {
                                Text(folder.title).foregroundColor(Theme.text)
                                Spacer()
                                if viewModel.itemFolders.contains(folder.id) { Image(systemName: "checkmark").foregroundColor(Theme.accent) }
                            }
                        }
                    }
                }
                Section("Новая папка") {
                    TextField("Название", text: $newTitle)
                    Button("Создать и добавить") {
                        let title = newTitle
                        newTitle = ""
                        Task { await viewModel.createFolder(title: title) }
                    }
                    .disabled(newTitle.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
            .navigationTitle("В папку")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Готово") { dismiss() } } }
        }
        .presentationDetents([.medium, .large])
    }
}
