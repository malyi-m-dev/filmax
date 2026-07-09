import SwiftUI
import Shared

/// Плеер для Apple TV: нативный `AVPlayerViewController` (пультовый транспорт, аудио/субтитры
/// из HLS) + возобновление и сохранение прогресса общим слоем (см. `PlayerSurface`).
struct TvPlayerView: View {
    @StateObject private var viewModel: PlayerViewModel

    init(itemId: Int32, videoId: Int32?) {
        _viewModel = StateObject(wrappedValue: PlayerViewModel(itemId: itemId, videoId: videoId))
    }

    var body: some View {
        PlayerSurface(viewModel: viewModel)
            .ignoresSafeArea()
    }
}
