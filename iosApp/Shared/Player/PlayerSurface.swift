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
    /// URL, для которого уже создан текущий плеер — чтобы не пересоздавать его дважды на старте.
    @State private var currentURL: URL?

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
        .onDisappear {
            teardownPlayer(saveProgress: true)
            currentURL = nil
        }
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
        guard let url = viewModel.streamURL, url != currentURL else { return }
        // Позиция для возобновления: текущая позиция прежнего плеера (смена качества «на лету»)
        // либо сохранённый прогресс из истории — берём большую, чтобы не откатиться назад.
        let previous = player?.currentTime().seconds ?? 0
        let resumeAt = max(viewModel.startSeconds, previous.isFinite ? previous : 0)

        teardownPlayer(saveProgress: false)
        currentURL = url

        let newPlayer = AVPlayer(url: url)
        if resumeAt > 1 {
            newPlayer.seek(to: CMTime(seconds: resumeAt, preferredTimescale: 1))
        }
        addProgressObserver(to: newPlayer)
        newPlayer.play()
        player = newPlayer
    }

    private func addProgressObserver(to player: AVPlayer) {
        // Периодически (раз в 15 сек) пишем прогресс через общий слой.
        let interval = CMTime(seconds: 15, preferredTimescale: 1)
        timeObserver = player.addPeriodicTimeObserver(forInterval: interval, queue: .main) { time in
            let seconds = time.seconds
            // CMTime может быть NaN/indefinite (плеер не готов) — Int(nan) роняет приложение.
            guard seconds.isFinite, seconds > 0 else { return }
            Task { await viewModel.saveProgress(seconds: Int(seconds)) }
        }
    }

    private func reload() async {
        currentURL = nil
        await viewModel.load()
        setupPlayer()
    }

    private func teardownPlayer(saveProgress: Bool) {
        if let player, let timeObserver {
            player.removeTimeObserver(timeObserver)
            if saveProgress {
                let seconds = player.currentTime().seconds
                if seconds.isFinite, seconds > 0 {
                    Task { await viewModel.saveProgress(seconds: Int(seconds)) }
                }
            }
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
