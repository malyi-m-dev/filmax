import SwiftUI
import Shared

/// Открытая папка-закладка: сетка тайтлов; «Убрать из папки» — контекстным меню.
struct FolderView: View {
    @StateObject private var viewModel: FolderViewModel
    let title: String

    init(folderId: Int32, title: String) {
        _viewModel = StateObject(wrappedValue: FolderViewModel(folderId: folderId))
        self.title = title
    }

    var body: some View {
        ScrollView {
            if let error = viewModel.error {
                ErrorView(message: error) { Task { await viewModel.retry() } }
            } else if viewModel.items.isEmpty && !viewModel.isLoading {
                EmptyStateView(message: "Папка пуста", systemImage: "folder")
            } else {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 108), spacing: DS.Spacing.md, alignment: .top)], spacing: DS.Spacing.lg) {
                    ForEach(viewModel.items, id: \.id) { item in
                        NavigationLink(value: AppRoute.details(itemId: item.id)) {
                            PosterCard(posterURL: item.posterURL, title: item.title, subtitle: item.posterMeta,
                                       ratingText: item.ratingText, width: 108)
                        }
                        .buttonStyle(.plain)
                        .onAppear { if item.id == viewModel.items.last?.id { Task { await viewModel.loadNext() } } }
                        .contextMenu {
                            Button(role: .destructive) {
                                Task { await viewModel.remove(item) }
                            } label: { Label("Убрать из папки", systemImage: "folder.badge.minus") }
                        }
                    }
                }
                .padding(DS.Spacing.md)
            }
        }
        .background(Theme.background.ignoresSafeArea())
        .navigationTitle(title)
        .task { if viewModel.items.isEmpty { await viewModel.loadNext() } }
    }
}
