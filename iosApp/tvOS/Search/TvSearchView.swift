import SwiftUI
import Shared

/// Поиск для Apple TV: TV-клавиатура (`.searchable`) + focus-сетка результатов поверх `SearchViewModel`.
struct TvSearchView: View {
    @StateObject private var viewModel = SearchViewModel()

    private let columns = [GridItem(.adaptive(minimum: 240), spacing: DS.Spacing.xl)]

    var body: some View {
        content
            .searchable(text: $viewModel.query, prompt: "Название, актёр, режиссёр")
            .background(Theme.background.ignoresSafeArea())
    }

    @ViewBuilder private var content: some View {
        if viewModel.isLoading {
            TvLoadingView()
        } else if let error = viewModel.error {
            TvErrorView(message: error, systemImage: "magnifyingglass")
        } else if viewModel.isIdle {
            TvEmptyStateView(message: "Найдите фильм, сериал или аниме", systemImage: "magnifyingglass")
        } else if viewModel.results.isEmpty {
            TvEmptyStateView(message: "Ничего не найдено", systemImage: "magnifyingglass")
        } else {
            grid
        }
    }

    private var grid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: DS.Spacing.xxl) {
                ForEach(viewModel.results, id: \.id) { item in
                    TvPosterCard(
                        route: .details(itemId: item.id),
                        posterURL: item.posterURL,
                        title: item.title,
                        subtitle: item.metaLine,
                        ratingText: item.ratingText
                    )
                }
            }
            .padding(DS.tvSafeHorizontal)
        }
    }
}
