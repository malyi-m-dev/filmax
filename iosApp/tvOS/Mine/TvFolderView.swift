import SwiftUI
import Shared

/// Открытая папка-закладка: сетка тайтлов с постраничной догрузкой, «Убрать из папки» — долгим
/// нажатием на карточке.
struct TvFolderView: View {
    @StateObject private var viewModel: FolderViewModel
    let title: String

    init(folderId: Int32, title: String) {
        _viewModel = StateObject(wrappedValue: FolderViewModel(folderId: folderId))
        self.title = title
    }

    var body: some View {
        TvScreenScroll {
            TvScreenTitle(title: title)
            content
        }
        // Первая страница — один раз: при возврате с деталей догруженные страницы остаются.
        .task { if viewModel.items.isEmpty { await viewModel.loadNext() } }
    }

    @ViewBuilder private var content: some View {
        if !viewModel.items.isEmpty {
            TvGrid {
                ForEach(viewModel.items, id: \.id) { item in
                    TvPosterCard(item: item, menu: [removeAction(item)])
                        .onAppear {
                            guard item.id == viewModel.items.last?.id else { return }
                            Task { await viewModel.loadNext() }
                        }
                }
            }
        } else if viewModel.isLoading {
            TvLoadingView()
        } else if let error = viewModel.error {
            TvErrorView(message: error) { Task { await viewModel.retry() } }
        } else {
            TvEmptyStateView(message: "В папке пока пусто", systemImage: "folder")
        }
    }

    private func removeAction(_ item: Item) -> TvMenuAction {
        TvMenuAction(title: "Убрать из папки", systemImage: "folder.badge.minus", isDestructive: true) {
            Task { await viewModel.remove(item) }
        }
    }
}
