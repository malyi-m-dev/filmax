import SwiftUI
import Shared

/// Плеер iPhone/iPad. Другая серия — новый экран плеера (`.id`), а не подмена потока: прогресс
/// пишется в трек, выбранный при старте, как на Android.
struct PlayerView: View {
    let itemId: Int32
    @State private var target: PlayTarget?

    init(itemId: Int32, target: PlayTarget?) {
        self.itemId = itemId
        _target = State(initialValue: target)
    }

    var body: some View {
        PlayerScreen(itemId: itemId, target: target) { next in target = next }
            .id(target)
            .toolbar(.hidden, for: .tabBar)
    }
}

/// Один запуск плеера: своя модель на свой трек.
private struct PlayerScreen: View {
    @StateObject private var viewModel: PlayerViewModel
    let onNext: (PlayTarget) -> Void

    init(itemId: Int32, target: PlayTarget?, onNext: @escaping (PlayTarget) -> Void) {
        _viewModel = StateObject(wrappedValue: PlayerViewModel(itemId: itemId, target: target))
        self.onNext = onNext
    }

    var body: some View {
        PlayerSurface(viewModel: viewModel, onNext: onNext)
            .navigationTitle(viewModel.track.map { track in
                viewModel.item?.isEpisodic == true ? "\(viewModel.item?.title ?? "") · \(track.episodeCode)" : viewModel.item?.title ?? ""
            } ?? "")
            .navigationBarTitleDisplayMode(.inline)
    }
}
