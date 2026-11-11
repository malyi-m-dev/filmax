import SwiftUI

/// Фильмография актёра или режиссёра — сетка тайтлов, свежие сверху.
struct FilmographyView: View {
    @StateObject private var viewModel: FilmographyViewModel

    init(name: String, isDirector: Bool) {
        _viewModel = StateObject(wrappedValue: FilmographyViewModel(name: name, isDirector: isDirector))
    }

    var body: some View {
        ScrollView {
            if viewModel.isLoading {
                LoadingView().frame(minHeight: 300)
            } else if let error = viewModel.error {
                ErrorView(message: error) { Task { await viewModel.load() } }
            } else if viewModel.items.isEmpty {
                EmptyStateView(message: "Других тайтлов не нашлось", systemImage: "person")
            } else {
                ItemGrid(items: viewModel.items).padding(.top, DS.Spacing.sm)
            }
        }
        .background(Theme.background.ignoresSafeArea())
        .navigationTitle(viewModel.name)
        .task { await viewModel.load() }
    }
}
