import SwiftUI
import Shared

/// Библиотека для iPhone/iPad: Продолжить / Избранное / Закладки / Загрузки поверх `LibraryViewModel`.
struct LibraryView: View {
    @StateObject private var viewModel = LibraryViewModel()
    @State private var showNewFolder = false
    @State private var newFolderTitle = ""

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.history.isEmpty && viewModel.folders.isEmpty {
                LoadingView()
            } else if isEmpty {
                EmptyStateView(message: "Библиотека пуста — добавьте фильмы в избранное или начните смотреть", systemImage: "square.stack")
            } else {
                content
            }
        }
        .navigationTitle("Библиотека")
        .background(Theme.background.ignoresSafeArea())
        .task { await viewModel.load() }
        .task { await viewModel.observeFavorites() }
        .task { await viewModel.observeDownloads() }
        .toolbar {
            Button {
                showNewFolder = true
            } label: {
                Image(systemName: "folder.badge.plus")
            }
            .tint(Theme.accent)
        }
        .alert("Новая папка", isPresented: $showNewFolder) {
            TextField("Название", text: $newFolderTitle)
            Button("Создать") {
                let title = newFolderTitle
                newFolderTitle = ""
                Task { await viewModel.createFolder(title: title) }
            }
            Button("Отмена", role: .cancel) { newFolderTitle = "" }
        }
    }

    private var isEmpty: Bool {
        viewModel.history.isEmpty && viewModel.favorites.isEmpty
            && viewModel.downloads.isEmpty && viewModel.folders.isEmpty
    }

    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DS.Spacing.xl) {
                if !viewModel.history.isEmpty {
                    HistoryRail(title: "Продолжить просмотр", items: viewModel.history)
                }
                if !viewModel.favorites.isEmpty {
                    localRail(title: "Избранное", items: viewModel.favorites.map {
                        LocalCard(id: $0.id, title: $0.title, poster: $0.posterSmall)
                    })
                }
                if !viewModel.folders.isEmpty {
                    bookmarks
                }
                if !viewModel.downloads.isEmpty {
                    localRail(title: "Загрузки", items: viewModel.downloads.map {
                        LocalCard(id: $0.id, title: $0.title, poster: $0.posterSmall)
                    })
                }
            }
            .padding(.vertical, DS.Spacing.md)
        }
    }

    // MARK: Закладки (папки + содержимое in-place)

    private var bookmarks: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            SectionHeader(title: "Закладки").padding(.horizontal, DS.Spacing.md)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.sm) {
                    ForEach(viewModel.folders, id: \.id) { folder in
                        Button {
                            Task { await viewModel.openFolder(folder) }
                        } label: {
                            HStack(spacing: DS.Spacing.xs) {
                                Image(systemName: "folder.fill")
                                Text(folder.title)
                                Text("\(folder.count)").foregroundColor(Theme.onSurfaceVariant)
                            }
                            .font(.system(size: 14, weight: .semibold))
                            .padding(.horizontal, DS.Spacing.md)
                            .padding(.vertical, DS.Spacing.sm)
                            .background(viewModel.selectedFolder?.id == folder.id ? Theme.accent : Theme.surface)
                            .foregroundColor(.white)
                            .clipShape(Capsule())
                        }
                        .contextMenu {
                            Button("Удалить папку", role: .destructive) {
                                Task { await viewModel.deleteFolder(folder) }
                            }
                        }
                    }
                }
                .padding(.horizontal, DS.Spacing.md)
            }
            if viewModel.selectedFolder != nil {
                if viewModel.folderItems.isEmpty {
                    Text("Папка пуста")
                        .font(.system(size: 14))
                        .foregroundColor(Theme.onSurfaceVariant)
                        .padding(.horizontal, DS.Spacing.md)
                } else {
                    ItemRail(title: viewModel.selectedFolder?.title ?? "", items: viewModel.folderItems)
                }
            }
        }
    }

    // MARK: Рельса локальных элементов (избранное/загрузки)

    private struct LocalCard: Identifiable {
        let id: Int32
        let title: String
        let poster: String
    }

    private func localRail(title: String, items: [LocalCard]) -> some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            SectionHeader(title: title).padding(.horizontal, DS.Spacing.md)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DS.Spacing.md) {
                    ForEach(items) { card in
                        NavigationLink(value: AppRoute.details(itemId: card.id)) {
                            PosterCard(posterURL: card.poster, title: card.title)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, DS.Spacing.md)
            }
        }
    }
}
