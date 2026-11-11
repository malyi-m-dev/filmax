import SwiftUI
import Shared

/// Главная iPhone/iPad: герой с «Смотреть» и «Буду смотреть», «Продолжить», ряды ленты и
/// тематические ряды — тот же состав, что у Android.
struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()

    var body: some View {
        Group {
            if let feed = viewModel.feed, feed.hasContent {
                content(feed)
            } else if let error = viewModel.error {
                ErrorView(message: error) { Task { await viewModel.load() } }
            } else {
                LoadingView()
            }
        }
        .background(Theme.background.ignoresSafeArea())
        .navigationTitle("Главная")
        .toolbar(.hidden, for: .navigationBar)
        .task { if viewModel.feed == nil { await viewModel.load() } }
        .task { await viewModel.observeWatchlist() }
        .refreshable { await viewModel.load() }
    }

    private func content(_ feed: HomeFeed) -> some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: DS.Spacing.xl) {
                if feed.fromCache { OfflineBanner() }
                if let hero = feed.hero { HeroCard(item: hero, viewModel: viewModel) }
                if !feed.continueWatching.isEmpty { ContinueRail(entries: feed.continueWatching) }
                if !feed.fresh.isEmpty { ItemRail(title: "Новинки", items: feed.fresh) }
                if !feed.trending.isEmpty { ItemRail(title: "Популярные фильмы", items: feed.trending) }
                if !feed.forYou.isEmpty { ItemRail(title: "Сериалы с высоким рейтингом", items: feed.forYou) }
                if !feed.collections.isEmpty { CollectionsRail(collections: feed.collections) }
                ForEach(viewModel.shelves) { shelf in
                    ItemRail(title: shelf.kind.title, items: shelf.items) {
                        Task { await viewModel.loadMore(shelf) }
                    }
                }
            }
            .padding(.bottom, DS.Spacing.xl)
        }
    }
}

/// Герой: кадр на всю ширину, название и два действия.
private struct HeroCard: View {
    let item: Item
    @ObservedObject var viewModel: HomeViewModel

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            PosterImage(url: item.backdropURL)
                .aspectRatio(16 / 10, contentMode: .fill)
                .frame(maxWidth: .infinity)
                .clipped()
                .overlay(LinearGradient(colors: [.clear, Theme.background], startPoint: .center, endPoint: .bottom))
            VStack(alignment: .leading, spacing: DS.Spacing.sm) {
                Text(item.title).font(.system(size: 28, weight: .bold)).foregroundColor(Theme.text)
                Text(item.metaLine).font(.subheadline).foregroundColor(Theme.textSecondary)
                HStack(spacing: DS.Spacing.sm) {
                    NavigationLink(value: AppRoute.player(itemId: item.id, target: nil)) {
                        Label("Смотреть", systemImage: "play.fill").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
                    let marked = viewModel.watchlistIds.contains(item.id)
                    Button {
                        Task { await viewModel.toggleWatchlist(item) }
                    } label: {
                        Label(marked ? "В списке" : "Буду смотреть", systemImage: marked ? "bookmark.fill" : "bookmark")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .tint(Theme.text)
                }
                .controlSize(.large)
                NavigationLink(value: AppRoute.details(itemId: item.id)) {
                    Text("Подробнее").font(.footnote.weight(.semibold))
                }
                .tint(Theme.accent)
            }
            .padding(DS.Spacing.md)
        }
    }
}

/// «Продолжить» ведёт сразу в плеер — на недосмотренную серию.
private struct ContinueRail: View {
    let entries: [WatchHistory]

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            SectionHeader(title: "Продолжить").padding(.horizontal, DS.Spacing.md)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: DS.Spacing.md) {
                    ForEach(entries, id: \.itemId) { entry in
                        NavigationLink(value: AppRoute.player(itemId: entry.itemId, target: entry.target)) {
                            ContinueCard(entry: entry)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, DS.Spacing.md)
            }
        }
    }
}

/// Карточка 16:9: кадр серии, подпись серии и полоса прогресса.
private struct ContinueCard: View {
    let entry: WatchHistory

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.xs) {
            PosterImage(url: entry.wideOrPoster)
                .aspectRatio(16 / 9, contentMode: .fill)
                .frame(width: 220, height: 124)
                .clipShape(RoundedRectangle(cornerRadius: DS.Radius.md))
                .overlay(alignment: .bottom) {
                    if let fraction = entry.progressFraction {
                        ProgressView(value: fraction).tint(Theme.accent).padding(DS.Spacing.sm)
                    }
                }
            Text(entry.title).font(.subheadline.weight(.semibold)).foregroundColor(Theme.text).lineLimit(1)
            Text(entry.subtitle).font(.caption).foregroundColor(Theme.textSecondary)
        }
        .frame(width: 220)
    }
}

private struct CollectionsRail: View {
    let collections: [FilmCollection]

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            HStack {
                SectionHeader(title: "Подборки")
                NavigationLink("Все", value: AppRoute.collectionsList).tint(Theme.accent)
            }
            .padding(.horizontal, DS.Spacing.md)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: DS.Spacing.md) {
                    ForEach(collections, id: \.id) { collection in
                        NavigationLink(value: AppRoute.collection(id: collection.id, title: collection.title)) {
                            PosterCard(posterURL: collection.posterURL, title: collection.title, width: 150)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, DS.Spacing.md)
            }
        }
    }
}
