import SwiftUI
import Shared

/// Главная Apple TV поверх общего `HomeViewModel`: герой, «Продолжить», ряды ленты и тематические
/// ряды с догрузкой — тот же состав, что у Главной на Android TV.
struct TvHomeView: View {
    @StateObject private var viewModel = HomeViewModel()

    var body: some View {
        content
            .background(Theme.background.ignoresSafeArea())
            // Лента грузится один раз: перезагрузка при возврате с деталей сбросила бы догруженные
            // страницы рядов, и фокус потерял бы карточку, с которой ушли.
            .task { if viewModel.feed == nil { await viewModel.load() } }
            .task { await viewModel.observeWatchlist() }
    }

    @ViewBuilder private var content: some View {
        if let feed = viewModel.feed, feed.hasContent {
            TvScreenScroll {
                TvHomeNotice(feed: feed)
                if let hero = feed.hero {
                    TvHomeHero(item: hero, inWatchlist: viewModel.watchlistIds.contains(hero.id)) {
                        Task { await viewModel.toggleWatchlist(hero) }
                    }
                }
                TvHomeFeedRails(feed: feed)
                TvHomeShelves(viewModel: viewModel)
            }
        } else if let error = viewModel.error, !viewModel.isLoading {
            TvErrorView(message: error) { Task { await viewModel.load() } }
        } else {
            TvLoadingView()
        }
    }
}

/// Мягкая ошибка: часть ленты не пришла или она из кэша без сети. Контент остаётся, баннер — сверху.
private struct TvHomeNotice: View {
    let feed: HomeFeed

    var body: some View {
        if let message {
            Label(message, systemImage: "wifi.exclamationmark")
                .font(TvType.caption)
                .foregroundColor(Theme.textSecondary)
                .padding(.horizontal, DS.tvSafeHorizontal)
                .padding(.top, DS.Spacing.md)
        }
    }

    private var message: String? {
        if feed.fromCache { return "Нет сети — показана сохранённая лента" }
        return feed.error
    }
}

// MARK: - Герой

private struct TvHomeHero: View {
    let item: Item
    let inWatchlist: Bool
    let onToggleWatchlist: () -> Void

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            TvBackdrop(url: item.backdropURL, height: TvMetrics.heroHeight)
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
            meta
            if !item.plot.isEmpty {
                Text(item.plot)
                    .font(TvType.body)
                    .foregroundColor(Theme.textSecondary)
                    .lineLimit(3)
                    .frame(maxWidth: TvMetrics.textColumnWidth, alignment: .leading)
            }
            actions.padding(.top, DS.Spacing.sm)
        }
    }

    private var meta: some View {
        HStack(spacing: DS.Spacing.md) {
            if !item.ratingLines.isEmpty {
                Text(item.ratingLines.joined(separator: " · "))
                    .foregroundColor(Theme.accent2)
            }
            Text(item.metaLine)
                .foregroundColor(Theme.textSecondary)
        }
        .font(TvType.body)
    }

    private var actions: some View {
        HStack(spacing: DS.Spacing.lg) {
            // Без адреса плеер сам выберет: фильм целиком, у сериала — серию «продолжить».
            NavigationLink(value: AppRoute.player(itemId: item.id, target: nil)) {
                Label("Смотреть", systemImage: "play.fill")
            }
            .tvActionStyle(.primary)
            Button(action: onToggleWatchlist) {
                Label(inWatchlist ? "В списке" : "Буду смотреть", systemImage: inWatchlist ? "checkmark" : "plus")
            }
            .tvActionStyle(.secondary)
            NavigationLink(value: AppRoute.details(itemId: item.id)) {
                Label("Подробнее", systemImage: "info.circle")
            }
            .tvActionStyle(.secondary)
        }
        .focusSection()
    }
}

// MARK: - Ряды ленты

private struct TvHomeFeedRails: View {
    let feed: HomeFeed

    var body: some View {
        if !feed.continueWatching.isEmpty {
            TvRail("Продолжить") {
                ForEach(feed.continueWatching, id: \.itemId) { entry in
                    TvPosterCard(history: entry)
                }
            }
        }
        itemRail("Новинки", feed.fresh)
        itemRail("Популярное", feed.trending)
        itemRail("Для вас", feed.forYou)
        if !feed.collections.isEmpty {
            TvRail("Подборки", allRoute: .collectionsList) {
                ForEach(feed.collections, id: \.id) { collection in
                    TvPosterCard(collection: collection)
                }
            }
        }
    }

    @ViewBuilder private func itemRail(_ title: String, _ items: [Item]) -> some View {
        if !items.isEmpty {
            TvRail(title) {
                ForEach(items, id: \.id) { item in
                    TvPosterCard(item: item)
                }
            }
        }
    }
}

/// Тематические ряды (`HomeShelves`): приходят после ленты и листаются дальше постранично.
private struct TvHomeShelves: View {
    @ObservedObject var viewModel: HomeViewModel

    var body: some View {
        ForEach(viewModel.shelves) { shelf in
            TvRail(shelf.kind.title) {
                ForEach(shelf.items, id: \.id) { item in
                    TvPosterCard(item: item)
                        .onAppear {
                            guard item.id == shelf.items.last?.id else { return }
                            Task { await viewModel.loadMore(shelf) }
                        }
                }
            }
        }
    }
}
