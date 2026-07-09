import SwiftUI
import Shared

/// Экран Деталей фильма/сериала для Apple TV (крупный бэкдроп, focus на действиях и сериях).
struct TvDetailsView: View {
    @StateObject private var viewModel: DetailsViewModel

    init(itemId: Int32) {
        _viewModel = StateObject(wrappedValue: DetailsViewModel(itemId: itemId))
    }

    var body: some View {
        Group {
            if let item = viewModel.item {
                content(item)
            } else if let error = viewModel.error {
                TvErrorView(message: error) { Task { await viewModel.load() } }
            } else {
                TvLoadingView()
            }
        }
        .background(Theme.background.ignoresSafeArea())
        .task { if viewModel.item == nil { await viewModel.load() } }
        .task { await viewModel.observeFavorite() }
    }

    private func content(_ item: Item) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DS.Spacing.xxl) {
                header(item)
                if item.isSeries {
                    TvSeasonEpisodeBrowser(itemId: item.id, tracks: item.tracklist)
                }
                if !viewModel.similar.isEmpty {
                    TvItemRail(title: "Похожие", items: viewModel.similar)
                }
            }
            .padding(.horizontal, DS.tvSafeHorizontal)
            .padding(.vertical, DS.tvSafeVertical)
        }
    }

    private func header(_ item: Item) -> some View {
        HStack(alignment: .top, spacing: DS.Spacing.xxl) {
            PosterImage(url: item.posterURL)
                .aspectRatio(posterAspectRatio, contentMode: .fill)
                .frame(width: 340, height: 340 / posterAspectRatio)
                .clipShape(RoundedRectangle(cornerRadius: DS.Radius.xl))

            VStack(alignment: .leading, spacing: DS.Spacing.lg) {
                Text(item.title)
                    .font(.system(size: 56, weight: .heavy))
                    .foregroundColor(.white)
                HStack(spacing: DS.Spacing.md) {
                    if let rating = item.ratingText {
                        Label(rating, systemImage: "star.fill")
                            .font(.system(size: 24, weight: .semibold))
                            .foregroundColor(Theme.accent)
                    }
                    Text(item.metaLine)
                        .font(.system(size: 24))
                        .foregroundColor(Theme.onSurfaceVariant)
                }
                if !item.plot.isEmpty {
                    Text(item.plot)
                        .font(.system(size: 22))
                        .foregroundColor(Theme.onSurfaceVariant)
                        .lineLimit(5)
                }
                actions(item)
                metaRow("Режиссёр", item.director)
                metaRow("В ролях", item.cast)
            }
        }
    }

    private func actions(_ item: Item) -> some View {
        HStack(spacing: DS.Spacing.lg) {
            NavigationLink(value: AppRoute.player(itemId: item.id, videoId: nil)) {
                Label("Смотреть", systemImage: "play.fill").padding(.horizontal, DS.Spacing.lg)
            }
            .buttonStyle(.borderedProminent).tint(Theme.accent)

            Button {
                Task { await viewModel.toggleFavorite() }
            } label: {
                Label("В избранное", systemImage: viewModel.isFavorite ? "heart.fill" : "heart")
            }
            .buttonStyle(.bordered).tint(Theme.accent)

            Button {
                Task { await viewModel.toggleWatchlist() }
            } label: {
                Label("Буду смотреть", systemImage: viewModel.inWatchlist ? "checkmark" : "plus")
            }
            .buttonStyle(.bordered).tint(Theme.accent)
        }
    }

    @ViewBuilder private func metaRow(_ label: String, _ value: String) -> some View {
        if !value.isEmpty {
            Text("\(label): \(value)")
                .font(.system(size: 20))
                .foregroundColor(Theme.onSurfaceVariant)
                .lineLimit(2)
        }
    }
}

/// Браузер сезонов/серий для Apple TV — focus-сетка серий выбранного сезона.
struct TvSeasonEpisodeBrowser: View {
    let itemId: Int32
    let tracks: [MediaTrack]
    @State private var selectedSeason: Int32

    init(itemId: Int32, tracks: [MediaTrack]) {
        self.itemId = itemId
        self.tracks = tracks
        _selectedSeason = State(initialValue: tracks.first?.seasonNumber ?? 1)
    }

    private var seasons: [Int32] { Array(Set(tracks.map { $0.seasonNumber })).sorted() }
    private var episodes: [MediaTrack] {
        tracks.filter { $0.seasonNumber == selectedSeason }.sorted { $0.number < $1.number }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.lg) {
            TvSectionHeader(title: "Серии")
            if seasons.count > 1 {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: DS.Spacing.md) {
                        ForEach(seasons, id: \.self) { season in
                            Button {
                                selectedSeason = season
                            } label: {
                                Text("Сезон \(season)").padding(.horizontal, DS.Spacing.md)
                            }
                            .buttonStyle(.bordered)
                            .tint(season == selectedSeason ? Theme.accent : Theme.onSurfaceVariant)
                        }
                    }
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.xl) {
                    ForEach(episodes, id: \.id) { episode in
                        TvPosterCard(
                            route: .player(itemId: itemId, videoId: episode.id),
                            posterURL: episode.thumbnail,
                            title: episode.title.isEmpty ? "Серия \(episode.number)" : episode.title,
                            subtitle: "Серия \(episode.number)",
                            width: 300
                        )
                    }
                }
                .padding(.vertical, DS.Spacing.lg)
            }
        }
    }
}
