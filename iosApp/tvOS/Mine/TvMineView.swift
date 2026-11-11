import SwiftUI
import Shared

/// «Моё» Apple TV — как на Android TV: «Продолжить · Буду смотреть · Папки · История». Папок у
/// домашнего сервера нет — сегмента тоже нет (это решает `MineViewModel.segments`).
struct TvMineView: View {
    @StateObject private var viewModel = MineViewModel()

    var body: some View {
        TvScreenScroll {
            segmentPicker
            TvMineSegmentContent(viewModel: viewModel)
        }
        // История перечитывается при каждом возврате: после плеера «Продолжить» должно быть свежим.
        .task { await viewModel.load() }
        .task { await viewModel.observeWatchlist() }
    }

    private var segmentPicker: some View {
        Picker("Раздел", selection: $viewModel.segment) {
            ForEach(viewModel.segments) { segment in
                Text(segment.rawValue).tag(segment)
            }
        }
        .pickerStyle(.segmented)
        .frame(maxWidth: TvMetrics.textColumnWidth)
        .padding(.horizontal, DS.tvSafeHorizontal)
        .padding(.top, DS.Spacing.lg)
        .focusSection()
    }
}

private struct TvMineSegmentContent: View {
    @ObservedObject var viewModel: MineViewModel

    var body: some View {
        switch viewModel.segment {
        case .continueWatching:
            historyGrid(viewModel.continueWatching, emptyMessage: "Начатые фильмы и сериалы появятся здесь")
        case .watchlist:
            watchlistGrid
        case .folders:
            foldersGrid
        case .history:
            historyGrid(viewModel.history, emptyMessage: "История просмотров пуста")
        }
    }

    @ViewBuilder private func historyGrid(_ entries: [WatchHistory], emptyMessage: String) -> some View {
        if viewModel.isLoading && viewModel.history.isEmpty {
            TvLoadingView()
        } else if let error = viewModel.error, viewModel.history.isEmpty {
            TvErrorView(message: error) { Task { await viewModel.load() } }
        } else if entries.isEmpty {
            TvEmptyStateView(message: emptyMessage, systemImage: "clock.arrow.circlepath")
        } else {
            TvGrid(shape: .wide) {
                ForEach(entries, id: \.itemId) { entry in
                    TvPosterCard(history: entry, menu: [forgetAction(entry)])
                }
            }
        }
    }

    @ViewBuilder private var watchlistGrid: some View {
        if viewModel.watchlist.isEmpty {
            TvEmptyStateView(message: "Добавляйте тайтлы кнопкой «Буду смотреть»", systemImage: "bookmark")
        } else {
            TvGrid {
                ForEach(viewModel.watchlist, id: \.id) { entry in
                    TvPosterCard(watchlistItem: entry, menu: [removeAction(entry)])
                }
            }
        }
    }

    @ViewBuilder private var foldersGrid: some View {
        if viewModel.isLoading && viewModel.folders.isEmpty {
            TvLoadingView()
        } else if viewModel.folders.isEmpty {
            TvEmptyStateView(message: "Папок пока нет — их создают в деталях тайтла кнопкой «В папку»", systemImage: "folder")
        } else {
            TvGrid {
                ForEach(viewModel.folders, id: \.id) { folder in
                    TvPosterCard(
                        route: .folder(id: folder.id, title: folder.title),
                        posterURL: viewModel.folderPosters[folder.id]?.first,
                        title: folder.title,
                        subtitle: titlesCount(folder.count),
                        placeholderIcon: "folder"
                    )
                }
            }
        }
    }

    private func forgetAction(_ entry: WatchHistory) -> TvMenuAction {
        TvMenuAction(title: "Убрать из истории", systemImage: "trash", isDestructive: true) {
            Task { await viewModel.forget(entry) }
        }
    }

    private func removeAction(_ entry: WatchlistItem) -> TvMenuAction {
        TvMenuAction(title: "Убрать", systemImage: "bookmark.slash", isDestructive: true) {
            Task { await viewModel.removeFromWatchlist(entry.id) }
        }
    }
}

/// «1 тайтл», «3 тайтла», «12 тайтлов» — русское склонение по последним цифрам.
private func titlesCount(_ count: Int32) -> String {
    let lastTwo = count % 100
    let last = count % 10
    if (11...14).contains(lastTwo) { return "\(count) тайтлов" }
    switch last {
    case 1: return "\(count) тайтл"
    case 2...4: return "\(count) тайтла"
    default: return "\(count) тайтлов"
    }
}
