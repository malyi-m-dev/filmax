import SwiftUI
import Shared

/// Выдача поиска внутри Каталога: запрос набирают системной клавиатурой `.searchable`, а здесь —
/// подсказка, загрузка, ошибка или сетка найденного.
struct TvSearchResultsView: View {
    @ObservedObject var viewModel: SearchViewModel

    var body: some View {
        content
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Theme.background.ignoresSafeArea())
    }

    @ViewBuilder private var content: some View {
        if viewModel.isIdle {
            TvEmptyStateView(message: "Введите хотя бы два символа", systemImage: "magnifyingglass")
        } else if viewModel.isLoading && viewModel.results.isEmpty {
            TvLoadingView()
        } else if let error = viewModel.error {
            TvErrorView(message: error, systemImage: "magnifyingglass")
        } else if viewModel.results.isEmpty {
            TvEmptyStateView(message: "Ничего не найдено", systemImage: "magnifyingglass")
        } else {
            TvScreenScroll {
                TvGrid {
                    ForEach(viewModel.results, id: \.id) { item in
                        TvPosterCard(item: item)
                    }
                }
            }
        }
    }
}
