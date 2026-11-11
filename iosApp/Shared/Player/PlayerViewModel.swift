import Combine
import Foundation
import Shared

/// Плеер (iOS и tvOS). Видео играет нативный `AVPlayer` во View, а здесь — что играть: трек по
/// адресу, потоки под этот запуск (`StreamRepository`: домашний сервер выдаёт ссылки на каждый
/// старт), качество, запасные варианты доставки, следующая серия и прогресс.
@MainActor
final class PlayerViewModel: ObservableObject {
    let itemId: Int32

    @Published var item: Item?
    @Published var track: MediaTrack?
    @Published var streamURL: URL?
    @Published var qualities: [String] = []
    @Published var currentQuality: String?
    @Published var nextTarget: PlayTarget?
    @Published var error: String?
    @Published var isLoading = false
    /// Стартовая позиция (сек): сохранённый прогресс; досмотренное — сначала, а не с титров.
    @Published var startSeconds: Double = 0

    private let target: PlayTarget?
    private let catalog = RepositoryProvider.shared.catalog
    private let streams = RepositoryProvider.shared.streams
    private let watching = RepositoryProvider.shared.watching
    private let playback = RepositoryProvider.shared.playbackSettings

    /// Варианты доставки текущего качества: hls4 → hls → http (разные CDN).
    private var variants: [URL] = []
    private var variantIndex = 0
    private var lastSentSeconds: Int?

    /// Порог отправки прогресса — реже тика плеера, но чаще, чем теряется место (как на Android).
    private static let progressStepSeconds = 5
    private static let auto = PlaybackSettings.Companion.shared.QualityAuto

    init(itemId: Int32, target: PlayTarget?) {
        self.itemId = itemId
        self.target = target
    }

    func load() async {
        isLoading = true
        error = nil
        let result: Loaded<Item> = await request(fallback: "Не удалось получить тайтл.") {
            try await self.catalog.getItemDetails(id: self.itemId)
        }
        guard case .value(let loaded) = result else {
            if case .failure(let message) = result { error = message }
            isLoading = false
            return
        }
        item = loaded
        guard let listed = pick(from: loaded) else {
            error = "У тайтла нет видео."
            isLoading = false
            return
        }
        let playable: Loaded<MediaTrack> = await request(fallback: "Не удалось получить поток.") {
            try await self.streams.playable(itemId: self.itemId, track: listed)
        }
        guard case .value(let ready) = playable else {
            if case .failure(let message) = playable { error = message }
            isLoading = false
            return
        }
        track = ready
        startSeconds = ready.watchStatus == 1 ? 0 : Double(ready.watchedSeconds)
        nextTarget = next(after: ready, in: loaded)?.target
        qualities = ready.files.map { $0.quality }
        await start(quality: preferredQuality(ready.files))
        isLoading = false
    }

    /// Смена качества — с той же позиции (её держит View) и с первого варианта доставки.
    func select(quality: String) async {
        guard quality != currentQuality else { return }
        await start(quality: quality)
    }

    /// Поток не пошёл — следующий вариант доставки того же качества; false — варианты кончились.
    func streamFailed() -> Bool {
        guard variantIndex + 1 < variants.count else {
            error = "Не удалось воспроизвести видео."
            return false
        }
        variantIndex += 1
        streamURL = variants[variantIndex]
        return true
    }

    /// Прогресс — по НОМЕРУ видео с сезоном (`VideoRef`), а не по id трека.
    func saveProgress(seconds: Int) async {
        guard let track, seconds > 0 else { return }
        if let sent = lastSentSeconds, abs(seconds - sent) < Self.progressStepSeconds { return }
        lastSentSeconds = seconds
        _ = try? await watching.saveProgress(itemId: itemId, video: track.target.ref, timeSeconds: Int32(seconds))
    }

    /// Трек адреса; без адреса — серия «продолжить» у сериала, первое видео у фильма.
    private func pick(from item: Item) -> MediaTrack? {
        if let target, let match = item.tracklist.first(where: { target.matches($0) }) { return match }
        return item.isEpisodic ? item.resumeTrack : item.tracklist.first
    }

    private func next(after track: MediaTrack, in item: Item) -> MediaTrack? {
        guard let index = item.tracklist.firstIndex(where: { $0.seasonNumber == track.seasonNumber && $0.number == track.number }),
              item.tracklist.indices.contains(index + 1) else { return nil }
        return item.tracklist[index + 1]
    }

    private func start(quality: String?) async {
        guard let file = track?.files.first(where: { $0.quality == quality }) ?? track?.files.first else {
            error = "Нет доступного потока."
            return
        }
        currentQuality = file.quality
        variants = [file.hls4, file.hls, file.http].compactMap { $0 }.compactMap { URL(string: $0) }
        variantIndex = 0
        streamURL = variants.first
        if streamURL == nil { error = "Нет доступного потока." }
    }

    /// Качество из настроек; «Авто» или такого нет — лучшее доступное.
    private func preferredQuality(_ files: [VideoFile]) async -> String? {
        var preferred = Self.auto
        for await settings in playback.settings {
            preferred = settings.quality
            break
        }
        let labels = files.map { $0.quality }
        if preferred != Self.auto, labels.contains(preferred) { return preferred }
        return labels.max { height($0) < height($1) }
    }

    /// Высота кадра из подписи качества: «1080p» → 1080, «4K» → 2160.
    private func height(_ label: String) -> Int {
        if label.uppercased().contains("4K") { return 2160 }
        return Int(label.filter { $0.isNumber }) ?? 0
    }
}
