import SwiftUI
import Shared

/// Поиск iPhone/iPad: системное поле `.searchable`, недавние запросы и подсказки, пока запрос
/// короткий, иначе — сетка результатов.
struct SearchView: View {
    @StateObject private var viewModel = SearchViewModel()

    var body: some View {
        ScrollView {
            if viewModel.isIdle {
                suggestions
            } else if viewModel.isLoading && viewModel.results.isEmpty {
                LoadingView().frame(minHeight: 300)
            } else if let error = viewModel.error {
                ErrorView(message: error)
            } else if viewModel.results.isEmpty {
                EmptyStateView(message: "Ничего не нашлось", systemImage: "magnifyingglass")
            } else {
                ItemGrid(items: viewModel.results).padding(.top, DS.Spacing.sm)
            }
        }
        .background(Theme.background.ignoresSafeArea())
        .navigationTitle("Поиск")
        .searchable(text: $viewModel.query, prompt: "Фильмы, сериалы, актёры")
        // В недавние — отправленный запрос, а не каждая набранная подстрока.
        .onSubmit(of: .search) { viewModel.remember() }
        .autocorrectionDisabled()
    }

    private var suggestions: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.lg) {
            if !viewModel.recent.isEmpty { querySection(title: "Недавние", queries: viewModel.recent, icon: "clock") }
            querySection(title: "Часто ищут", queries: viewModel.trending, icon: "flame")
        }
        .padding(DS.Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func querySection(title: String, queries: [String], icon: String) -> some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            SectionHeader(title: title)
            ForEach(queries, id: \.self) { query in
                Button {
                    viewModel.query = query
                } label: {
                    Label(query, systemImage: icon).foregroundColor(Theme.text)
                }
                .padding(.vertical, DS.Spacing.xs)
            }
        }
    }
}
