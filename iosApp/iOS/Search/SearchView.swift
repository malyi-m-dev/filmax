import SwiftUI
import Shared

/// Поиск для iPhone/iPad: поле ввода (`.searchable`) + сетка результатов поверх `SearchViewModel`.
struct SearchView: View {
    @StateObject private var viewModel = SearchViewModel()

    private let columns = [GridItem(.adaptive(minimum: 110), spacing: DS.Spacing.md)]

    var body: some View {
        Group {
            if viewModel.isLoading {
                LoadingView()
            } else if let error = viewModel.error {
                ErrorView(message: error, systemImage: "magnifyingglass")
            } else if viewModel.isIdle {
                EmptyStateView(message: "Найдите фильм, сериал или аниме", systemImage: "magnifyingglass")
            } else if viewModel.results.isEmpty {
                EmptyStateView(message: "Ничего не найдено", systemImage: "magnifyingglass")
            } else {
                grid
            }
        }
        .navigationTitle("Поиск")
        .background(Theme.background.ignoresSafeArea())
        .searchable(text: $viewModel.query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Название, актёр, режиссёр")
    }

    private var grid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: DS.Spacing.lg) {
                ForEach(viewModel.results, id: \.id) { item in
                    NavigationLink(value: AppRoute.details(itemId: item.id)) {
                        PosterCard(
                            posterURL: item.posterURL,
                            title: item.title,
                            subtitle: item.metaLine,
                            ratingText: item.ratingText,
                            width: 110
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(DS.Spacing.md)
        }
    }
}
