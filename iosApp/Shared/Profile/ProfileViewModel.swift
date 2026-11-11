import Combine
import Shared

/// Профиль (iOS и tvOS): аккаунт, подписка (только у источника с подпиской), настройки
/// воспроизведения, источник контента и выход — как Профиль на Android.
@MainActor
final class ProfileViewModel: ObservableObject {
    @Published var profile: UserProfile?
    @Published var settings: PlaybackSettings?
    @Published var source: ContentSource = .kinopub
    @Published var isLoading = false
    @Published var error: String?

    let qualityOptions: [String] = PlaybackSettings.Companion.shared.qualityOptions
    let audioOptions: [String] = PlaybackSettings.Companion.shared.audioOptions
    let subtitleOptions: [String] = PlaybackSettings.Companion.shared.subtitleOptions
    let sourceOptions: [ContentSource] = [.kinopub, .jellyfin]

    private let user = RepositoryProvider.shared.user
    private let playback = RepositoryProvider.shared.playbackSettings
    private let sources = RepositoryProvider.shared.sources
    private let logoutUseCase = UseCaseProvider.shared.logoutUseCase()

    /// Есть ли у источника подписка: у домашнего сервера её нет — карточка показывает сам сервер.
    var hasSubscription: Bool { CatalogKit.shared.capabilities(source: source).subscription }

    /// Подпись под именем: «Подписка до 12 декабря · 47 дн.» или название источника.
    var accountCaption: String {
        guard hasSubscription else { return source.label }
        guard let subscription = profile?.subscription, subscription.active else { return "Подписка не активна" }
        guard let days = subscription.daysLeft?.int32Value else { return "Подписка активна" }
        return "Подписка активна · осталось \(days) дн."
    }

    func load() async {
        source = sources.source.value
        isLoading = true
        error = nil
        let result: Loaded<UserProfile> = await request(fallback: "Не удалось загрузить профиль.") {
            try await self.user.getProfile()
        }
        switch result {
        case .value(let loaded): profile = loaded
        case .failure(let message): error = message
        }
        isLoading = false
    }

    func observeSettings() async {
        for await value in playback.settings { settings = value }
    }

    func setQuality(_ value: String) async { try? await playback.setQuality(quality: value) }
    func setAudio(_ value: String) async { try? await playback.setAudioLanguage(language: value) }
    func setSubtitles(_ value: String) async { try? await playback.setSubtitleLanguage(language: value) }

    /// Смена источника: сессия пересоберёт корень — ко входу, если в новый источник не входили.
    func select(_ next: ContentSource) async {
        guard next != source else { return }
        try? await sources.select(source: next)
    }

    /// Выход — только из текущего источника: вход в другой сохраняется.
    func logout() async {
        _ = try? await logoutUseCase.invoke()
    }
}
