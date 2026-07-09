import SwiftUI
import Shared

/// Экран Деталей фильма/сериала для iPhone/iPad.
struct DetailsView: View {
    @StateObject private var viewModel: DetailsViewModel

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
        .task {
            if viewModel.item == nil { await viewModel.load() }
        }
        .task { await viewModel.observeFavorite() }
    }

    private func content(_ item: Item) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DS.Spacing.lg) {
                backdrop(item)
                VStack(alignment: .leading, spacing: DS.Spacing.lg) {
                    actions(item)
                    metadata(item)
                    if !item.plot.isEmpty {
                        Text(item.plot)
                            .font(.system(size: 15))
                            .foregroundColor(Theme.onSurfaceVariant)
                    }
                    if item.isSeries {
                        SeasonEpisodeBrowser(itemId: item.id, tracks: item.tracklist)
                    }
                    if !viewModel.similar.isEmpty {
                        ItemRail(title: "Похожие", items: viewModel.similar)
                            .padding(.horizontal, -DS.Spacing.md)
                    }
                }
                .padding(.horizontal, DS.Spacing.md)
            }
            .padding(.bottom, DS.Spacing.xl)
        }
    }

    private func backdrop(_ item: Item) -> some View {
        ZStack(alignment: .bottomLeading) {
            PosterImage(url: item.backdropURL)
                .aspectRatio(16.0 / 9.0, contentMode: .fill)
                .frame(maxWidth: .infinity)
                .frame(height: 240)
                .clipped()
                .overlay(
                    LinearGradient(colors: [.clear, Theme.background], startPoint: .top, endPoint: .bottom)
                )
            Text(item.title)
                .font(.system(size: 28, weight: .heavy))
                .foregroundColor(.white)
                .padding(DS.Spacing.md)
        }
    }

    private func actions(_ item: Item) -> some View {
        HStack(spacing: DS.Spacing.sm) {
            NavigationLink(value: AppRoute.player(itemId: item.id, videoId: nil)) {
                Label("Смотреть", systemImage: "play.fill")
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, DS.Spacing.sm + 2)
                    .background(Theme.accent)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: DS.Radius.md))
            }
            iconButton(viewModel.isFavorite ? "heart.fill" : "heart") {
                Task { await viewModel.toggleFavorite() }
            }
            iconButton(viewModel.inWatchlist ? "checkmark" : "plus") {
                Task { await viewModel.toggleWatchlist() }
            }
        }
    }

    private func iconButton(_ systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 48, height: 48)
                .background(Theme.surface)
                .clipShape(RoundedRectangle(cornerRadius: DS.Radius.md))
        }
    }

    private func metadata(_ item: Item) -> some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            HStack(spacing: DS.Spacing.sm) {
                if let rating = item.ratingText {
                    Label(rating, systemImage: "star.fill")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(Theme.accent)
                }
                Text(item.metaLine)
                    .font(.system(size: 13))
                    .foregroundColor(Theme.onSurfaceVariant)
            }
            metaRow("Режиссёр", item.director)
            metaRow("В ролях", item.cast)
            metaRow("Страна", item.country)
        }
    }

    @ViewBuilder private func metaRow(_ label: String, _ value: String) -> some View {
        if !value.isEmpty {
            HStack(alignment: .top, spacing: DS.Spacing.sm) {
                Text(label)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(Theme.onSurfaceVariant)
                    .frame(width: 90, alignment: .leading)
                Text(value)
                    .font(.system(size: 13))
                    .foregroundColor(.white)
            }
        }
    }
}

/// Браузер сезонов/серий сериала: выбор сезона + список серий → Плеер по `videoId` серии.
struct SeasonEpisodeBrowser: View {
    let itemId: Int32
    let tracks: [MediaTrack]
    @State private var selectedSeason: Int32

    init(itemId: Int32, tracks: [MediaTrack]) {
        self.itemId = itemId
        self.tracks = tracks
        _selectedSeason = State(initialValue: tracks.first?.seasonNumber ?? 1)
    }

    private var seasons: [Int32] {
        Array(Set(tracks.map { $0.seasonNumber })).sorted()
    }

    private var episodes: [MediaTrack] {
        tracks.filter { $0.seasonNumber == selectedSeason }.sorted { $0.number < $1.number }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.md) {
            SectionHeader(title: "Серии")
            if seasons.count > 1 {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: DS.Spacing.sm) {
                        ForEach(seasons, id: \.self) { season in
                            Button {
                                selectedSeason = season
                            } label: {
                                Text("Сезон \(season)")
                                    .font(.system(size: 14, weight: .semibold))
                                    .padding(.horizontal, DS.Spacing.md)
                                    .padding(.vertical, DS.Spacing.sm)
                                    .background(season == selectedSeason ? Theme.accent : Theme.surface)
                                    .foregroundColor(.white)
                                    .clipShape(Capsule())
                            }
                        }
                    }
                }
            }
            ForEach(episodes, id: \.id) { episode in
                NavigationLink(value: AppRoute.player(itemId: itemId, videoId: episode.id)) {
                    EpisodeRow(episode: episode)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

private struct EpisodeRow: View {
    let episode: MediaTrack
    var body: some View {
        HStack(spacing: DS.Spacing.md) {
            PosterImage(url: episode.thumbnail, placeholderIcon: "play.rectangle")
                .aspectRatio(16.0 / 9.0, contentMode: .fill)
                .frame(width: 120, height: 68)
                .clipShape(RoundedRectangle(cornerRadius: DS.Radius.sm))
            VStack(alignment: .leading, spacing: DS.Spacing.xs) {
                Text(episode.title.isEmpty ? "Серия \(episode.number)" : episode.title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
                    .lineLimit(1)
                Text("Серия \(episode.number)")
                    .font(.system(size: 12))
                    .foregroundColor(Theme.onSurfaceVariant)
            }
            Spacer()
            Image(systemName: "play.circle.fill")
                .font(.system(size: 24))
                .foregroundColor(Theme.accent)
        }
        .padding(.vertical, DS.Spacing.xs)
    }
}
