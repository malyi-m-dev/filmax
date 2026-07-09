import SwiftUI
import Shared

/// Плеер для iPhone/iPad: нативный `AVPlayer` + прогресс/качество из общего слоя (см. `PlayerSurface`).
struct PlayerView: View {
    @StateObject private var viewModel: PlayerViewModel

    init(itemId: Int32, videoId: Int32?) {
        _viewModel = StateObject(wrappedValue: PlayerViewModel(itemId: itemId, videoId: videoId))
    }

    var body: some View {
        PlayerSurface(viewModel: viewModel)
            .navigationTitle(viewModel.item?.title ?? "")
            .navigationBarTitleDisplayMode(.inline)
    }
}
