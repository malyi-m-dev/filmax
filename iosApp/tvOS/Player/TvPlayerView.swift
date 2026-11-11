import SwiftUI
import Shared

/// Плеер Apple TV: нативный `AVPlayerViewController` (пультовый транспорт, озвучка и субтитры
/// потока) через общий `PlayerSurface`. Следующая серия — новый запуск плеера, а не подмена потока:
/// прогресс пишется в трек, выбранный при старте, как на Android.
struct TvPlayerView: View {
    let itemId: Int32
    /// Текущая серия. Заменить экран в `NavigationStack` без пути нельзя, поэтому смена серии
    /// пересоздаёт сессию плеера через `.id`: новая модель, новый поток, старый пишет прогресс
    /// и закрывается.
    @State private var target: PlayTarget?

    init(itemId: Int32, target: PlayTarget?) {
        self.itemId = itemId
        _target = State(initialValue: target)
    }

    var body: some View {
        TvPlayerSession(itemId: itemId, target: target) { next in
            target = next
        }
        .id(target)
        .ignoresSafeArea()
    }
}

/// Один запуск плеера: модель живёт ровно столько, сколько эта View с её `.id`.
private struct TvPlayerSession: View {
    @StateObject private var viewModel: PlayerViewModel
    let onNext: (PlayTarget) -> Void

    init(itemId: Int32, target: PlayTarget?, onNext: @escaping (PlayTarget) -> Void) {
        _viewModel = StateObject(wrappedValue: PlayerViewModel(itemId: itemId, target: target))
        self.onNext = onNext
    }

    var body: some View {
        PlayerSurface(viewModel: viewModel, onNext: onNext)
    }
}
