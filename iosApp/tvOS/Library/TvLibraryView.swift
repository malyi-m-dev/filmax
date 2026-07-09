import SwiftUI
import Shared

/// Библиотека для Apple TV: Продолжить / Избранное / Закладки / Загрузки на focus engine.
struct TvLibraryView: View {
    @StateObject private var viewModel = LibraryViewModel()

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.history.isEmpty && viewModel.folders.isEmpty {
                TvLoadingView()
            } else if isEmpty {
                TvEmptyStateView(message: "Библиотека пуста — добавьте фильмы в избранное или начните смотреть", systemImage: "square.stack")
            } else {
                content
            }
        }
        .background(Theme.background.ignoresSafeArea())
        .task { await viewModel.load() }
        .task { await viewModel.observeFavorites() }
        .task { await viewModel.observeDownloads() }
    }

    private var isEmpty: Bool {
        viewModel.history.isEmpty && viewModel.favorites.isEmpty
            && viewModel.downloads.isEmpty && viewModel.folders.isEmpty
    }

    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DS.Spacing.xxl) {
                if !viewModel.history.isEmpty {
                    TvHistoryRail(title: "Продолжить просмотр", items: viewModel.history)
                }
                if !viewModel.favorites.isEmpty {
                    localRail(title: "Избранное", cards: viewModel.favorites.map {
                        LocalCard(id: $0.id, title: $0.title, poster: $0.posterSmall)
                    })
                }
                if !viewModel.folders.isEmpty { bookmarks }
                if !viewModel.downloads.isEmpty {
                    localRail(title: "Загрузки", cards: viewModel.downloads.map {
                        LocalCard(id: $0.id, title: $0.title, poster: $0.posterSmall)
                    })
                }
            }
            .padding(.horizontal, DS.tvSafeHorizontal)
            .padding(.vertical, DS.tvSafeVertical)
        }
    }

    private var bookmarks: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.md) {
            TvSectionHeader(title: "Закладки")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.md) {
                    ForEach(viewModel.folders, id: \.id) { folder in
                        Button {
                            Task { await viewModel.openFolder(folder) }
                        } label: {
                            Label("\(folder.title) (\(folder.count))", systemImage: "folder.fill")
                                .padding(.horizontal, DS.Spacing.md)
                        }
                        .buttonStyle(.bordered)
                        .tint(viewModel.selectedFolder?.id == folder.id ? Theme.accent : Theme.onSurfaceVariant)
                    }
                }
            }
            if viewModel.selectedFolder != nil {
                if viewModel.folderItems.isEmpty {
                    Text("Папка пуста").font(.system(size: 24)).foregroundColor(Theme.onSurfaceVariant)
                } else {
                    TvItemRail(title: viewModel.selectedFolder?.title ?? "", items: viewModel.folderItems)
                }
            }
        }
    }

    private struct LocalCard: Identifiable {
        let id: Int32
        let title: String
        let poster: String
    }

    private func localRail(title: String, cards: [LocalCard]) -> some View {
        VStack(alignment: .leading, spacing: DS.Spacing.md) {
            TvSectionHeader(title: title)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.xl) {
                    ForEach(cards) { card in
                        TvPosterCard(
                            route: .details(itemId: card.id),
                            posterURL: card.poster,
                            title: card.title
                        )
                    }
                }
                .padding(.vertical, DS.Spacing.lg)
            }
        }
    }
}
