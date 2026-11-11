import AVKit
import Combine
import SwiftUI
import Shared

/// `AVPlayerViewController` в SwiftUI: системные контролы, выбор озвучки и субтитров потока,
/// AirPlay и картинка в картинке — нативно, как в приложениях Apple.
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

/// Поверхность плеера, общая для iOS и tvOS: ставит поток из [PlayerViewModel] в `AVPlayer`,
/// переносит позицию при смене качества и варианта доставки, пишет прогресс и по концу серии
/// просит следующую ([onNext]) — другая серия это навигация, а не подмена потока, как на Android.
struct PlayerSurface: View {
    @ObservedObject var viewModel: PlayerViewModel
    let onNext: (PlayTarget) -> Void

    @State private var player: AVPlayer?
    @State private var timeObserver: Any?
    @State private var currentURL: URL?
    @State private var statusWatch: AnyCancellable?
    @State private var endWatch: AnyCancellable?

    /// Как часто View отдаёт позицию модели; реже отправку прореживает сама модель.
    private static let progressTick = CMTime(seconds: 5, preferredTimescale: 1)

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if let player {
                AVPlayerContainer(player: player)
                    .ignoresSafeArea()
                    .overlay(alignment: .topTrailing) { controls }
            } else if let error = viewModel.error {
                ErrorOverlay(message: error) { Task { await reload() } }
            } else {
                ProgressView().tint(Theme.accent).scaleEffect(1.5)
            }
        }
        .task {
            if viewModel.item == nil { await viewModel.load() }
            setupPlayer()
        }
        .onChange(of: viewModel.streamURL) { _ in setupPlayer() }
        .onDisappear {
            teardownPlayer(saveProgress: true)
            currentURL = nil
        }
    }

    @ViewBuilder private var controls: some View {
        HStack(spacing: DS.Spacing.sm) {
            #if os(iOS)
            if viewModel.qualities.count > 1 {
                Menu {
                    ForEach(viewModel.qualities, id: \.self) { quality in
                        Button {
                            Task { await viewModel.select(quality: quality) }
                        } label: {
                            if quality == viewModel.currentQuality {
                                Label(quality, systemImage: "checkmark")
                            } else {
                                Text(quality)
                            }
                        }
                    }
                } label: {
                    circleIcon("slider.horizontal.3")
                }
            }
            #endif
            if let next = viewModel.nextTarget {
                Button { onNext(next) } label: { circleIcon("forward.end.fill") }
                    .accessibilityLabel("Следующая серия")
            }
        }
        .padding(DS.Spacing.lg)
    }

    private func circleIcon(_ name: String) -> some View {
        Image(systemName: name)
            .font(.system(size: 18, weight: .semibold))
            .foregroundColor(.white)
            .padding(DS.Spacing.md)
            .background(.black.opacity(0.5))
            .clipShape(Circle())
    }

    /// Новый поток с прежней позиции: смена качества или варианта доставки не отматывает видео.
    private func setupPlayer() {
        guard let url = viewModel.streamURL, url != currentURL else { return }
        let previous = player?.currentTime().seconds ?? 0
        let resumeAt = max(viewModel.startSeconds, previous.isFinite ? previous : 0)

        teardownPlayer(saveProgress: false)
        currentURL = url

        let item = AVPlayerItem(url: url)
        let newPlayer = AVPlayer(playerItem: item)
        if resumeAt > 1 {
            newPlayer.seek(to: CMTime(seconds: resumeAt, preferredTimescale: 1))
        }
        watch(item)
        addProgressObserver(to: newPlayer)
        newPlayer.play()
        player = newPlayer
    }

    /// Сбой потока — запасной вариант доставки (другой CDN); конец серии — следующая.
    private func watch(_ item: AVPlayerItem) {
        statusWatch = item.publisher(for: \.status)
            .receive(on: DispatchQueue.main)
            .sink { status in
                if status == .failed, viewModel.streamFailed() == false { teardownPlayer(saveProgress: true) }
            }
        endWatch = NotificationCenter.default.publisher(for: .AVPlayerItemDidPlayToEndTime, object: item)
            .receive(on: DispatchQueue.main)
            .sink { _ in
                if let next = viewModel.nextTarget { onNext(next) }
            }
    }

    private func addProgressObserver(to player: AVPlayer) {
        timeObserver = player.addPeriodicTimeObserver(forInterval: Self.progressTick, queue: .main) { time in
            let seconds = time.seconds
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
        statusWatch = nil
        endWatch = nil
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
                .font(.system(size: 44)).foregroundColor(Theme.error)
            Text(message).foregroundColor(.white).multilineTextAlignment(.center)
            Button("Повторить", action: retry)
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
        }
        .padding(DS.Spacing.xl)
    }
}
