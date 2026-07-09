import AVKit
import SwiftUI
import Shared

/// Обёртка нативного `AVPlayerViewController` для SwiftUI (iOS + tvOS).
/// Нативный контроллер даёт транспорт и встроенное меню аудио/субтитров из HLS «из коробки».
struct AVPlayerContainer: UIViewControllerRepresentable {
    let player: AVPlayer

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.player = player
        controller.videoGravity = .resizeAspect
        return controller
    }

    func updateUIViewController(_ controller: AVPlayerViewController, context: Context) {
        if controller.player !== player { controller.player = player }
    }
}

/// Общая «поверхность» плеера: создаёт `AVPlayer` из `PlayerViewModel`, возобновляет позицию,
/// периодически и на выходе сохраняет прогресс, даёт меню выбора качества. Платформенные View —
/// тонкие обёртки поверх неё.
struct PlayerSurface: View {
    @ObservedObject var viewModel: PlayerViewModel
    @State private var player: AVPlayer?
    @State private var timeObserver: Any?

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if let player {
                AVPlayerContainer(player: player)
                    .ignoresSafeArea()
                    .overlay(alignment: .topTrailing) { qualityMenu }
            } else if let error = viewModel.error {
                ErrorOverlay(message: error) { Task { await reload() } }
            } else {
                ProgressView().tint(Theme.accent).scaleEffect(1.5)
            }
        }
        .task {
            if viewModel.item == nil { await viewModel.load() }
            if player == nil { setupPlayer() }
        }
        // Пересоздаём плеер при смене URL (например, переключение качества из оверлея).
        .onChange(of: viewModel.streamURL) { _ in setupPlayer() }
        .onDisappear { teardown() }
    }

    @ViewBuilder private var qualityMenu: some View {
        // `Menu` доступен на tvOS только с 17.0 — на Apple TV качество меняется из Профиля,
        // а транспорт/аудио/субтитры даёт нативный контроллер. На iOS — меню поверх видео.
        #if os(iOS)
        if let settings = viewModel.settings, !viewModel.qualityOptions.isEmpty {
            Menu {
                ForEach(viewModel.qualityOptions, id: \.self) { quality in
                    Button {
                        Task { await viewModel.applyQuality(quality) }
                    } label: {
                        if quality == settings.quality {
                            Label(quality, systemImage: "checkmark")
                        } else {
                            Text(quality)
                        }
                    }
                }
            } label: {
                Image(systemName: "slider.horizontal.3")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(DS.Spacing.md)
                    .background(.black.opacity(0.5))
                    .clipShape(Circle())
            }
            .padding(DS.Spacing.lg)
        }
        #endif
    }

    // MARK: - Жизненный цикл плеера

    private func setupPlayer() {
        guard let url = viewModel.streamURL else { return }
        teardown()
        let newPlayer = AVPlayer(url: url)
        if viewModel.startSeconds > 1 {
            newPlayer.seek(to: CMTime(seconds: viewModel.startSeconds, preferredTimescale: 1))
        }
        addProgressObserver(to: newPlayer)
        newPlayer.play()
        player = newPlayer
    }

    private func addProgressObserver(to player: AVPlayer) {
        // Периодически (раз в 15 сек) пишем прогресс через общий слой.
        let interval = CMTime(seconds: 15, preferredTimescale: 1)
        timeObserver = player.addPeriodicTimeObserver(forInterval: interval, queue: .main) { time in
            let seconds = Int(time.seconds)
            guard seconds > 0 else { return }
            Task { await viewModel.saveProgress(seconds: seconds) }
        }
    }

    private func reload() async {
        await viewModel.load()
        setupPlayer()
    }

    private func teardown() {
        if let player, let timeObserver {
            player.removeTimeObserver(timeObserver)
            // Финальное сохранение позиции при выходе.
            let seconds = Int(player.currentTime().seconds)
            if seconds > 0 { Task { await viewModel.saveProgress(seconds: seconds) } }
        }
        timeObserver = nil
        player?.pause()
        player = nil
    }
}

private struct ErrorOverlay: View {
    let message: String
    let retry: () -> Void
    var body: some View {
        VStack(spacing: DS.Spacing.md) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 44)).foregroundColor(Theme.accent)
            Text(message).foregroundColor(.white).multilineTextAlignment(.center)
            Button("Повторить", action: retry).buttonStyle(.borderedProminent).tint(Theme.accent)
        }
        .padding(DS.Spacing.xl)
    }
}
