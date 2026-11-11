import SwiftUI
import Shared

/// «Моё» iPhone/iPad: сегменты «Продолжить · Буду смотреть · Папки · История» (папок у
/// домашнего сервера нет — сегмента тоже). Удаление — системным контекстным меню.
struct MineView: View {
    @StateObject private var viewModel = MineViewModel()
    @State private var newFolderOpen = false
    @State private var newFolderTitle = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DS.Spacing.md) {
                Picker("Раздел", selection: $viewModel.segment) {
                    ForEach(viewModel.segments) { segment in Text(segment.rawValue).tag(segment) }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, DS.Spacing.md)
                segmentContent
            }
            .padding(.vertical, DS.Spacing.sm)
        }
        .background(Theme.background.ignoresSafeArea())
        .navigationTitle("Моё")
        .toolbar {
            if viewModel.segment == .folders {
                Button { newFolderOpen = true } label: { Image(systemName: "folder.badge.plus") }
            }
        }
        .alert("Новая папка", isPresented: $newFolderOpen) {
            TextField("Название", text: $newFolderTitle)
            Button("Создать") {
                let title = newFolderTitle
                newFolderTitle = ""
                Task { await viewModel.createFolder(title: title) }
            }
            Button("Отмена", role: .cancel) { newFolderTitle = "" }
        }
        // Вернулись из плеера или деталей — история и папки могли измениться.
        .task { await viewModel.load() }
        .task { await viewModel.observeWatchlist() }
        .refreshable { await viewModel.load() }
    }

    @ViewBuilder private var segmentContent: some View {
        switch viewModel.segment {
        case .continueWatching: historyList(viewModel.continueWatching, empty: "Начатое появится здесь")
        case .history: historyList(viewModel.history, empty: "История пуста")
        case .watchlist: watchlistGrid
        case .folders: folderList
        }
    }

    @ViewBuilder private func historyList(_ entries: [WatchHistory], empty: String) -> some View {
        if viewModel.isLoading && entries.isEmpty {
            LoadingView().frame(minHeight: 240)
        } else if entries.isEmpty {
            EmptyStateView(message: empty, systemImage: "clock")
        } else {
            LazyVStack(spacing: DS.Spacing.sm) {
                ForEach(entries, id: \.itemId) { entry in
                    NavigationLink(value: AppRoute.player(itemId: entry.itemId, target: entry.target)) {
                        HistoryRow(entry: entry)
                    }
                    .buttonStyle(.plain)
                    .contextMenu {
                        NavigationLink(value: AppRoute.details(itemId: entry.itemId)) { Label("Подробнее", systemImage: "info.circle") }
                        Button(role: .destructive) {
                            Task { await viewModel.forget(entry) }
                        } label: { Label("Убрать из истории", systemImage: "trash") }
                    }
                }
            }
            .padding(.horizontal, DS.Spacing.md)
        }
    }

    @ViewBuilder private var watchlistGrid: some View {
        if viewModel.watchlist.isEmpty {
            EmptyStateView(message: "Отмечайте «Буду смотреть» в деталях", systemImage: "bookmark")
        } else {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 108), spacing: DS.Spacing.md, alignment: .top)], spacing: DS.Spacing.lg) {
                ForEach(viewModel.watchlist, id: \.id) { entry in
                    NavigationLink(value: AppRoute.details(itemId: entry.id)) {
                        PosterCard(posterURL: entry.posterSmall, title: entry.title, subtitle: entry.year > 0 ? "\(entry.year)" : nil,
                                   ratingText: ratingLabel(entry.rating?.doubleValue), width: 108)
                    }
                    .buttonStyle(.plain)
                    .contextMenu {
                        Button(role: .destructive) {
                            Task { await viewModel.removeFromWatchlist(entry.id) }
                        } label: { Label("Убрать из списка", systemImage: "bookmark.slash") }
                    }
                }
            }
            .padding(.horizontal, DS.Spacing.md)
        }
    }

    @ViewBuilder private var folderList: some View {
        if viewModel.folders.isEmpty {
            EmptyStateView(message: "Папок пока нет", systemImage: "folder")
        } else {
            LazyVStack(spacing: DS.Spacing.sm) {
                ForEach(viewModel.folders, id: \.id) { folder in
                    NavigationLink(value: AppRoute.folder(id: folder.id, title: folder.title)) {
                        FolderRow(folder: folder, posters: viewModel.folderPosters[folder.id] ?? [])
                    }
                    .buttonStyle(.plain)
                    .contextMenu {
                        Button(role: .destructive) {
                            Task { await viewModel.delete(folder: folder) }
                        } label: { Label("Удалить папку", systemImage: "trash") }
                    }
                }
            }
            .padding(.horizontal, DS.Spacing.md)
        }
    }
}

private struct HistoryRow: View {
    let entry: WatchHistory

    var body: some View {
        HStack(spacing: DS.Spacing.md) {
            PosterImage(url: entry.wideOrPoster)
                .aspectRatio(16 / 9, contentMode: .fill)
                .frame(width: 128, height: 72)
                .clipShape(RoundedRectangle(cornerRadius: DS.Radius.sm))
            VStack(alignment: .leading, spacing: DS.Spacing.xs) {
                Text(entry.title).font(.subheadline.weight(.semibold)).foregroundColor(Theme.text).lineLimit(2)
                Text(entry.subtitle).font(.caption).foregroundColor(Theme.textSecondary)
                if let fraction = entry.progressFraction { ProgressView(value: fraction).tint(Theme.accent) }
            }
            Spacer()
        }
    }
}

private struct FolderRow: View {
    let folder: BookmarkFolder
    let posters: [String]

    var body: some View {
        HStack(spacing: DS.Spacing.md) {
            PosterImage(url: posters.first, placeholderIcon: "folder")
                .aspectRatio(posterAspectRatio, contentMode: .fill)
                .frame(width: 56, height: 84)
                .clipShape(RoundedRectangle(cornerRadius: DS.Radius.sm))
            VStack(alignment: .leading, spacing: DS.Spacing.xs) {
                Text(folder.title).font(.headline).foregroundColor(Theme.text)
                Text("\(folder.count) тайтлов").font(.caption).foregroundColor(Theme.textSecondary)
            }
            Spacer()
            Image(systemName: "chevron.right").foregroundColor(Theme.onSurfaceVariant)
        }
        .padding(DS.Spacing.sm)
        .background(Theme.surface)
        .clipShape(RoundedRectangle(cornerRadius: DS.Radius.md))
    }
}
