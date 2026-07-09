import Combine
import Foundation
import Shared

/// Презентейшен плеера (iOS + tvOS). Общий слой отдаёт URL/треки и хранит прогресс — движок
/// нативный (`AVPlayer`) во View. Здесь: выбор трека/качества, применение `PlaybackSettings`,
/// сохранение прогресса через `WatchingRepository`.
@MainActor
final class PlayerViewModel: ObservableObject {
    let itemId: Int32
    let videoId: Int32?

    @Published var item: Item?
    @Published var track: MediaTrack?
    @Published var streamURL: URL?
    @Published var settings: PlaybackSettings?
    @Published var error: String?
    @Published var isLoading = false
    /// Стартовая позиция (сек) — возобновление с `MediaTrack.watchedSeconds`.
    @Published var startSeconds: Double = 0

    private let catalog = RepositoryProvider.shared.catalog
    private let watching = RepositoryProvider.shared.watching
    private let playback = RepositoryProvider.shared.playbackSettings

    init(itemId: Int32, videoId: Int32?) {
        self.itemId = itemId
        self.videoId = videoId
    }

    /// Доступные качества текущего трека (+ «Авто») — для меню в оверлее.
    var qualityOptions: [String] {
        var options = ["Авто"]
        options.append(contentsOf: (track?.files ?? []).map { $0.quality })
        return options
    }

    func load() async {
        isLoading = true
        error = nil
        let currentSettings = await firstSettings()
        settings = currentSettings
        do {
            let result = try await catalog.getItemDetails(id: itemId)
            switch onEnum(of: result) {
            case .success(let success):
                guard let loaded = success.data as? Item else {
                    error = "Некорректный ответ сервера."
                    isLoading = false
                    return
                }
                item = loaded
                resolveTrackAndURL(item: loaded, settings: currentSettings)
            case .error(let requestError):
                error = requestError.message ?? "Не удалось получить поток."
            }
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    /// Пересобрать URL после смены качества/дорожки из оверлея.
    func applyQuality(_ quality: String) async {
        try? await playback.setQuality(quality: quality)
        if let item, let updated = await firstSettings() {
            settings = updated
            resolveTrackAndURL(item: item, settings: updated)
        }
    }

    func setAudio(_ language: String) async { try? await playback.setAudioLanguage(language: language) }
    func setSubtitle(_ language: String) async { try? await playback.setSubtitleLanguage(language: language) }

    /// Сохранить прогресс (вызывается периодически из View и на выходе).
    func saveProgress(seconds: Int) async {
        guard let track else { return }
        if item?.isSeries == true {
            _ = try? await watching.saveProgressSerial(
                itemId: itemId, season: track.seasonNumber, videoId: track.id, timeSeconds: Int32(seconds)
            )
        } else {
            _ = try? await watching.saveProgress(
                itemId: itemId, videoId: track.id, timeSeconds: Int32(seconds)
            )
        }
    }

    // MARK: - Приватное

    private func resolveTrackAndURL(item: Item, settings: PlaybackSettings?) {
        let chosen = pickTrack(item.tracklist)
        track = chosen
        startSeconds = Double(chosen?.watchedSeconds ?? 0)
        streamURL = chosen.flatMap { resolveURL(files: $0.files, quality: settings?.quality) }
        if streamURL == nil {
            error = "Нет доступного потока для воспроизведения."
        }
    }

    private func pickTrack(_ tracklist: [MediaTrack]) -> MediaTrack? {
        if let videoId, let match = tracklist.first(where: { $0.id == videoId }) {
            return match
        }
        return tracklist.first
    }

    /// Выбор файла по качеству из настроек; «Авто»/нет совпадения → первый доступный.
    private func resolveURL(files: [VideoFile], quality: String?) -> URL? {
        let file: VideoFile?
        if let quality, quality != "Авто", let match = files.first(where: { $0.quality == quality }) {
            file = match
        } else {
            file = files.first
        }
        guard let file else { return nil }
        let urlString = file.hls ?? file.hls4 ?? file.http
        return urlString.flatMap { URL(string: $0) }
    }

    /// Берёт текущее значение настроек воспроизведения (первое эмиссия Flow).
    private func firstSettings() async -> PlaybackSettings? {
        for await value in playback.settings { return value }
        return nil
    }
}
