import SwiftUI
import Shared

// Секции деталей Apple TV. Каркас и состояние экрана — в `TvDetailsView`.

// MARK: - Шапка

/// Кадр во всю ширину, поверх — название, оценки, описание и действия. Фокус экрана начинается
/// на «Смотреть»: это первая кнопка ленты.
struct TvDetailsHeader: View {
    let item: Item
    @ObservedObject var viewModel: DetailsViewModel
    let showFolders: () -> Void

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            TvBackdrop(url: item.backdropURL, height: TvMetrics.detailsBackdropHeight)
            info
                .padding(.horizontal, DS.tvSafeHorizontal)
                .padding(.bottom, DS.Spacing.lg)
        }
    }

    private var info: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.md) {
            Text(item.title)
                .font(TvType.hero)
                .foregroundColor(Theme.text)
                .lineLimit(2)
            if !item.ratingLines.isEmpty {
                Text(item.ratingLines.joined(separator: " · "))
                    .font(TvType.body)
                    .fontWeight(.semibold)
                    .foregroundColor(Theme.accent2)
            }
            Text(metaText)
                .font(TvType.body)
                .foregroundColor(Theme.textSecondary)
            if !item.plot.isEmpty {
                Text(item.plot)
                    .font(TvType.body)
                    .foregroundColor(Theme.textSecondary)
                    .lineLimit(4)
                    .frame(maxWidth: TvMetrics.textColumnWidth, alignment: .leading)
            }
            actions.padding(.top, DS.Spacing.sm)
        }
    }

    /// «Сериал · 2021 · Фантастика, Драма · 2 ч 11 мин · США».
    private var metaText: String {
        let genres = item.genres.prefix(2).map { $0.title }.joined(separator: ", ")
        return [item.posterMeta, genres, item.durationText ?? "", item.country]
            .filter { !$0.isEmpty }
            .joined(separator: " · ")
    }

    private var actions: some View {
        HStack(spacing: DS.Spacing.lg) {
            // Фильм — целиком (адреса нет), сериал — серия «продолжить» с сезоном.
            NavigationLink(value: AppRoute.player(itemId: item.id, target: viewModel.playTarget)) {
                Label(viewModel.playTitle, systemImage: "play.fill")
            }
            .tvActionStyle(.primary)
            Button {
                Task { await viewModel.toggleWatchlist() }
            } label: {
                Label(
                    viewModel.inWatchlist ? "В списке" : "Буду смотреть",
                    systemImage: viewModel.inWatchlist ? "checkmark" : "plus"
                )
            }
            .tvActionStyle(.secondary)
            if viewModel.foldersAvailable {
                Button(action: showFolders) {
                    Label("В папку", systemImage: "folder.badge.plus")
                }
                .tvActionStyle(.secondary)
            }
        }
        .focusSection()
    }
}

// MARK: - Сезоны и серии

/// Чипы сезонов и ряд серий выбранного. Сезон меняется нажатием, а не фокусом: проход фокусом по
/// чипам не должен перестраивать ряд серий под пультом.
struct TvEpisodesSection: View {
    let item: Item
    @ObservedObject var viewModel: DetailsViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if item.seasonNumbers.count > 1 {
                TvChipRow(options: seasonOptions, selected: viewModel.selectedSeason) { season in
                    viewModel.selectedSeason = season
                }
            }
            TvRail(railTitle) {
                // Ключ — сезон и номер: номер серии уникален только внутри сезона.
                ForEach(viewModel.seasonEpisodes, id: \.target) { track in
                    TvPosterCard(itemId: item.id, track: track)
                }
            }
        }
    }

    private var seasonOptions: [TvChipOption<Int32?>] {
        item.seasonNumbers.map { TvChipOption<Int32?>(value: $0, title: "Сезон \($0)") }
    }

    private var railTitle: String {
        guard let season = viewModel.selectedSeason, season > 0 else { return "Серии" }
        return "Сезон \(season)"
    }
}

// MARK: - Люди

/// Режиссёр и актёры — кнопками: нажатие открывает фильмографию.
struct TvPeopleSection: View {
    let item: Item

    /// Дальше дюжины имён ряд превращается в титры, а не в навигацию.
    private static let castLimit = 12

    var body: some View {
        let directors = Self.names(item.director)
        let cast = Array(Self.names(item.cast).prefix(Self.castLimit))
        if !directors.isEmpty || !cast.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                TvSectionHeader(title: "Актёры и режиссёр")
                    .padding(.horizontal, DS.tvSafeHorizontal)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: DS.Spacing.md) {
                        ForEach(directors, id: \.self) { name in
                            TvPersonButton(name: name, isDirector: true)
                        }
                        ForEach(cast, id: \.self) { name in
                            TvPersonButton(name: name, isDirector: false)
                        }
                    }
                    .padding(.horizontal, DS.tvSafeHorizontal)
                    .padding(.vertical, DS.Spacing.lg)
                }
                .focusSection()
            }
        }
    }

    /// Имена из строки API «Имя, Имя»: без пустых и повторов — повтор ломает ключи `ForEach`.
    private static func names(_ list: String) -> [String] {
        var seen = Set<String>()
        return list.components(separatedBy: ",")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty && seen.insert($0).inserted }
    }
}

private struct TvPersonButton: View {
    let name: String
    let isDirector: Bool

    var body: some View {
        NavigationLink(value: AppRoute.filmography(name: name, isDirector: isDirector)) {
            VStack(alignment: .leading, spacing: DS.Spacing.xs) {
                Text(name).font(TvType.cardTitle)
                Text(isDirector ? "Режиссёр" : "Актёр").font(TvType.caption)
            }
        }
        .tvActionStyle(.secondary)
    }
}
