import SwiftUI
import Shared

/// Окно «В папку» (только у источника с папками): папки с отметкой, где тайтл уже лежит, и
/// создание новой — новая папка сразу получает тайтл, как «Создать и добавить» на Android.
struct TvFolderPickerView: View {
    @ObservedObject var viewModel: DetailsViewModel
    @State private var newTitle = ""
    @Environment(\.dismiss) private var dismiss

    init(viewModel: DetailsViewModel) {
        self.viewModel = viewModel
    }

    var body: some View {
        TvScreenScroll {
            TvScreenTitle(title: "В папку", subtitle: viewModel.item?.title)
            folders
            creation
            Button { dismiss() } label: { Label("Готово", systemImage: "checkmark") }
                .tvActionStyle(.primary)
                .padding(.horizontal, DS.tvSafeHorizontal)
        }
    }

    @ViewBuilder private var folders: some View {
        if viewModel.folders.isEmpty {
            TvFieldLabel(title: "Папок пока нет — создайте первую")
        } else {
            VStack(alignment: .leading, spacing: DS.Spacing.md) {
                ForEach(viewModel.folders, id: \.id) { folder in
                    TvFolderRow(folder: folder, isChecked: viewModel.itemFolders.contains(folder.id)) {
                        Task { await viewModel.toggle(folder: folder) }
                    }
                }
            }
            .padding(.horizontal, DS.tvSafeHorizontal)
            .focusSection()
        }
    }

    private var creation: some View {
        HStack(spacing: DS.Spacing.lg) {
            TextField("Новая папка", text: $newTitle)
                .autocorrectionDisabled()
                .frame(maxWidth: TvMetrics.formWidth)
            Button {
                let title = newTitle
                newTitle = ""
                Task { await viewModel.createFolder(title: title) }
            } label: {
                Label("Создать", systemImage: "folder.badge.plus")
            }
            .tvActionStyle(.secondary)
            .disabled(newTitle.trimmingCharacters(in: .whitespaces).isEmpty)
        }
        .padding(.horizontal, DS.tvSafeHorizontal)
        .focusSection()
    }
}

/// Строка папки: отметка — тайтл уже в ней, нажатие кладёт или убирает.
private struct TvFolderRow: View {
    let folder: BookmarkFolder
    let isChecked: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: DS.Spacing.md) {
                Image(systemName: isChecked ? "checkmark.circle.fill" : "circle")
                Text(folder.title)
                Spacer(minLength: DS.Spacing.lg)
                Text("\(folder.count)")
            }
            .frame(width: TvMetrics.formWidth)
        }
        .tvActionStyle(.secondary)
    }
}
