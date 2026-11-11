import SwiftUI
import Shared

/// Фильмография актёра или режиссёра — по нажатию на имя в деталях. Новые работы сверху.
struct TvFilmographyView: View {
    @StateObject private var viewModel: FilmographyViewModel

    init(name: String, isDirector: Bool) {
        _viewModel = StateObject(wrappedValue: FilmographyViewModel(name: name, isDirector: isDirector))
    }

    var body: some View {
        TvScreenScroll {
            TvScreenTitle(title: viewModel.name, subtitle: viewModel.isDirector ? "Режиссёр" : "Актёр")
            content
        }
        .task { await viewModel.load() }
    }

    @ViewBuilder private var content: some View {
        // Поиск по имени отдаёт тайтл столько раз, сколько он совпал; сетке нужны уникальные ключи.
        let items = viewModel.items.uniqueByID()
        if !items.isEmpty {
            TvGrid {
                ForEach(items, id: \.id) { item in
                    TvPosterCard(item: item)
                }
            }
        } else if viewModel.isLoading {
            TvLoadingView()
        } else if let error = viewModel.error {
            TvErrorView(message: error) { Task { await viewModel.load() } }
        } else {
            TvEmptyStateView(message: "Других работ не нашлось", systemImage: "person.crop.rectangle.stack")
        }
    }
}
