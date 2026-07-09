import SwiftUI
import Shared

/// Главная лента Apple TV: крупный hero + focus-рельсы поверх общего `HomeViewModel`.
struct TvHomeView: View {
    @StateObject private var viewModel = HomeViewModel()

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.feed == nil {
                TvLoadingView()
            } else if let error = viewModel.error, viewModel.feed?.hero == nil {
                TvErrorView(message: error) { Task { await viewModel.load() } }
            } else if let feed = viewModel.feed {
                content(feed)
            } else {
                TvLoadingView()
            }
        }
        .background(Theme.background.ignoresSafeArea())
        .task { if viewModel.feed == nil { await viewModel.load() } }
    }

    private func content(_ feed: HomeFeed) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DS.Spacing.xxl) {
                if let hero = feed.hero { TvHeroBanner(item: hero) }

                if !feed.continueWatching.isEmpty {
                    TvHistoryRail(title: "Продолжить просмотр", items: feed.continueWatching)
                }
                if !feed.trending.isEmpty {
                    TvItemRail(title: "В тренде", items: feed.trending)
                }
                if !feed.collections.isEmpty {
                    TvCollectionRail(title: "Подборки", collections: feed.collections)
                }
                if !feed.forYou.isEmpty {
                    TvItemRail(title: "Для вас", items: feed.forYou)
                }
            }
            .padding(.horizontal, DS.tvSafeHorizontal)
            .padding(.vertical, DS.tvSafeVertical)
        }
    }
}

// MARK: - Hero

private struct TvHeroBanner: View {
    let item: Item

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            PosterImage(url: item.backdropURL)
                .aspectRatio(16.0 / 9.0, contentMode: .fill)
                .frame(height: 520)
                .clipped()
                .overlay(
                    LinearGradient(
                        colors: [.clear, Theme.background.opacity(0.95)],
                        startPoint: .center, endPoint: .bottom
                    )
                )

            VStack(alignment: .leading, spacing: DS.Spacing.md) {
                Text(item.title)
                    .font(.system(size: 56, weight: .heavy))
                    .foregroundColor(.white)
                    .lineLimit(2)
                Text(item.metaLine)
                    .font(.system(size: 24))
                    .foregroundColor(Theme.onSurfaceVariant)
                HStack(spacing: DS.Spacing.lg) {
                    NavigationLink(value: AppRoute.player(itemId: item.id, videoId: nil)) {
                        Label("Смотреть", systemImage: "play.fill").padding(.horizontal, DS.Spacing.lg)
                    }
                    .buttonStyle(.borderedProminent).tint(Theme.accent)
                    NavigationLink(value: AppRoute.details(itemId: item.id)) {
                        Text("Подробнее").padding(.horizontal, DS.Spacing.lg)
                    }
                    .buttonStyle(.bordered).tint(Theme.accent)
                }
            }
            .padding(DS.Spacing.xl)
        }
        .clipShape(RoundedRectangle(cornerRadius: DS.Radius.xl))
    }
}

// MARK: - Рельсы

struct TvItemRail: View {
    let title: String
    let items: [Item]

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.md) {
            TvSectionHeader(title: title)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.xl) {
                    ForEach(items, id: \.id) { item in
                        TvPosterCard(
                            route: .details(itemId: item.id),
                            posterURL: item.posterURL,
                            title: item.title,
                            subtitle: item.metaLine,
                            ratingText: item.ratingText
                        )
                    }
                }
                .padding(.vertical, DS.Spacing.lg)
            }
        }
    }
}

struct TvHistoryRail: View {
    let title: String
    let items: [WatchHistory]

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.md) {
            TvSectionHeader(title: title)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.xl) {
                    ForEach(items, id: \.itemId) { history in
                        TvPosterCard(
                            route: .details(itemId: history.itemId),
                            posterURL: history.posterSmall,
                            title: history.title,
                            progress: history.progressFraction
                        )
                    }
                }
                .padding(.vertical, DS.Spacing.lg)
            }
        }
    }
}

struct TvCollectionRail: View {
    let title: String
    let collections: [FilmCollection]

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.md) {
            HStack {
                TvSectionHeader(title: title)
                NavigationLink(value: AppRoute.collectionsList) {
                    Text("Все").padding(.horizontal, DS.Spacing.md)
                }
                .buttonStyle(.bordered).tint(Theme.accent)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.xl) {
                    ForEach(collections, id: \.id) { collection in
                        TvPosterCard(
                            route: .collection(id: collection.id, title: collection.title),
                            posterURL: collection.posterURL,
                            title: collection.title
                        )
                    }
                }
                .padding(.vertical, DS.Spacing.lg)
            }
        }
    }
}
