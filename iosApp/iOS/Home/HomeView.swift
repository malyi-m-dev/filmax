import SwiftUI
import Shared

/// Главная лента iPhone/iPad: hero-баннер + горизонтальные рельсы поверх общего `HomeViewModel`.
struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.feed == nil {
                LoadingView()
            } else if let error = viewModel.error, viewModel.feed?.hero == nil {
                ErrorView(message: error) { Task { await viewModel.load() } }
            } else if let feed = viewModel.feed {
                content(feed)
            } else {
                LoadingView()
            }
        }
        .navigationTitle("Filmax")
        .navigationBarTitleDisplayMode(.inline)
        .background(Theme.background.ignoresSafeArea())
        .task { if viewModel.feed == nil { await viewModel.load() } }
        .refreshable { await viewModel.load() }
    }

    private func content(_ feed: HomeFeed) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DS.Spacing.xl) {
                if let softError = feed.error { softErrorBanner(softError) }
                if let hero = feed.hero { HeroBanner(item: hero) }

                if !feed.continueWatching.isEmpty {
                    HistoryRail(title: "Продолжить просмотр", items: feed.continueWatching)
                }
                if !feed.trending.isEmpty {
                    ItemRail(title: "В тренде", items: feed.trending)
                }
                if !feed.collections.isEmpty {
                    CollectionRail(title: "Подборки", collections: feed.collections)
                }
                if !feed.forYou.isEmpty {
                    ItemRail(title: "Для вас", items: feed.forYou)
                }
            }
            .padding(.vertical, DS.Spacing.md)
        }
    }

    private func softErrorBanner(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 13))
            .foregroundColor(Theme.onSurfaceVariant)
            .padding(.horizontal, DS.Spacing.md)
    }
}

// MARK: - Hero

private struct HeroBanner: View {
    let item: Item

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            PosterImage(url: item.backdropURL)
                .aspectRatio(16.0 / 9.0, contentMode: .fill)
                .frame(height: 220)
                .clipped()
                .overlay(
                    LinearGradient(
                        colors: [.clear, Theme.background.opacity(0.9)],
                        startPoint: .top, endPoint: .bottom
                    )
                )

            VStack(alignment: .leading, spacing: DS.Spacing.sm) {
                Text(item.title)
                    .font(.system(size: 26, weight: .heavy))
                    .foregroundColor(.white)
                    .lineLimit(2)
                Text(item.metaLine)
                    .font(.system(size: 13))
                    .foregroundColor(Theme.onSurfaceVariant)
                HStack(spacing: DS.Spacing.sm) {
                    NavigationLink(value: AppRoute.player(itemId: item.id, videoId: nil)) {
                        Label("Смотреть", systemImage: "play.fill")
                            .fontWeight(.semibold)
                            .padding(.horizontal, DS.Spacing.md)
                            .padding(.vertical, DS.Spacing.sm)
                            .background(Theme.accent)
                            .foregroundColor(.white)
                            .clipShape(Capsule())
                    }
                    NavigationLink(value: AppRoute.details(itemId: item.id)) {
                        Text("Подробнее")
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .padding(.horizontal, DS.Spacing.md)
                            .padding(.vertical, DS.Spacing.sm)
                            .background(.white.opacity(0.18))
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(DS.Spacing.md)
        }
        .clipShape(RoundedRectangle(cornerRadius: DS.Radius.lg))
        .padding(.horizontal, DS.Spacing.md)
    }
}

// MARK: - Рельсы

/// Горизонтальная рельса из `Item` (тренды/для вас).
struct ItemRail: View {
    let title: String
    let items: [Item]

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            SectionHeader(title: title).padding(.horizontal, DS.Spacing.md)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.md) {
                    ForEach(items, id: \.id) { item in
                        NavigationLink(value: AppRoute.details(itemId: item.id)) {
                            PosterCard(
                                posterURL: item.posterURL,
                                title: item.title,
                                subtitle: item.metaLine,
                                ratingText: item.ratingText
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, DS.Spacing.md)
            }
        }
    }
}

/// Рельса «Продолжить просмотр» из истории с прогресс-полосой.
struct HistoryRail: View {
    let title: String
    let items: [WatchHistory]

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            SectionHeader(title: title).padding(.horizontal, DS.Spacing.md)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.md) {
                    ForEach(items, id: \.itemId) { history in
                        NavigationLink(value: AppRoute.details(itemId: history.itemId)) {
                            PosterCard(
                                posterURL: history.posterSmall,
                                title: history.title,
                                progress: history.progressFraction
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, DS.Spacing.md)
            }
        }
    }
}

/// Рельса подборок.
struct CollectionRail: View {
    let title: String
    let collections: [FilmCollection]

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            HStack {
                Text(title).font(.system(size: 20, weight: .bold)).foregroundColor(.white)
                Spacer()
                NavigationLink(value: AppRoute.collectionsList) {
                    Text("Все").font(.system(size: 14, weight: .semibold)).foregroundColor(Theme.accent)
                }
            }
            .padding(.horizontal, DS.Spacing.md)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.md) {
                    ForEach(collections, id: \.id) { collection in
                        NavigationLink(value: AppRoute.collection(id: collection.id, title: collection.title)) {
                            PosterCard(posterURL: collection.posterURL, title: collection.title)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, DS.Spacing.md)
            }
        }
    }
}
