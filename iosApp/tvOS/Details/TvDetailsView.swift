import SwiftUI
import Shared

/// Детали Apple TV поверх общего `DetailsViewModel`: шапка с кадром и действиями, сезоны и серии,
/// актёры и режиссёр, «Похожие». Каркас и состояние — здесь, секции — в `TvDetailsSections`, окно
/// «В папку» — в `TvFolderPickerView`.
struct TvDetailsView: View {
    @StateObject private var viewModel: DetailsViewModel
    @State private var foldersShown = false

    init(itemId: Int32) {
        _viewModel = StateObject(wrappedValue: DetailsViewModel(itemId: itemId))
    }

    var body: some View {
        content
            .background(Theme.background.ignoresSafeArea())
            // Перечитывается при каждом возврате: после плеера у серий новый прогресс, а главная
            // кнопка — уже «Продолжить» следующей серии.
            .task { await viewModel.load() }
            .task { await viewModel.observeWatchlist() }
            .sheet(isPresented: $foldersShown) {
                TvFolderPickerView(viewModel: viewModel)
            }
    }

    @ViewBuilder private var content: some View {
        if let item = viewModel.item {
            TvDetailsContent(item: item, viewModel: viewModel) { foldersShown = true }
        } else if let error = viewModel.error {
            TvErrorView(message: error) { Task { await viewModel.load() } }
        } else {
            TvLoadingView()
        }
    }
}

/// Лента деталей загруженного тайтла.
private struct TvDetailsContent: View {
    let item: Item
    @ObservedObject var viewModel: DetailsViewModel
    let showFolders: () -> Void

    var body: some View {
        TvScreenScroll {
            TvDetailsHeader(item: item, viewModel: viewModel, showFolders: showFolders)
            // По типу, а не по числу видео: у фильма их тоже бывает несколько, и модель отдаёт их
            // как «серии» сезона 0.
            if item.isEpisodic && !viewModel.seasonEpisodes.isEmpty {
                TvEpisodesSection(item: item, viewModel: viewModel)
            }
            TvPeopleSection(item: item)
            if !viewModel.similar.isEmpty {
                TvRail("Похожие") {
                    ForEach(viewModel.similar, id: \.id) { similar in
                        TvPosterCard(item: similar)
                    }
                }
            }
        }
    }
}
