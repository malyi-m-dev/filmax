import Combine
import Shared

/// Презентейшен Профиля (iOS + tvOS): аккаунт, подписка, реальные настройки воспроизведения, выход.
/// Данные — общие `UserRepository` + `PlaybackSettingsRepository` + `LogoutUseCase`.
/// Никаких настроек-«пустышек» (учтён урок #41 на Android) — только то, что реально сохраняется.
@MainActor
final class ProfileViewModel: ObservableObject {
    @Published var profile: UserProfile?
    @Published var settings: PlaybackSettings?
    @Published var isLoading = false
    @Published var error: String?

    private let user = RepositoryProvider.shared.user
    private let playback = RepositoryProvider.shared.playbackSettings
    private let logout = UseCaseProvider.shared.logoutUseCase()

    /// Списки опций настроек воспроизведения (значения совпадают с `PlaybackSettings.Companion`
    /// в общем слое; сами значения сохраняются/читаются через `PlaybackSettingsRepository`).
    let qualityOptions = ["Авто", "2160p", "1080p", "720p", "480p", "360p"]
    let audioOptions = ["Оригинал", "Русский", "English"]
    let subtitleOptions = ["Выкл", "Русский", "English"]

    func load() async {
        isLoading = true
        error = nil
        do {
            let result = try await user.getProfile()
            switch onEnum(of: result) {
            case .success(let success):
                profile = success.data as? UserProfile
            case .error(let requestError):
                error = requestError.message ?? "Не удалось загрузить профиль."
            }
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    /// Реактивно держит текущие настройки воспроизведения из общего слоя (Flow).
    func observeSettings() async {
        for await value in playback.settings { settings = value }
    }

    func setQuality(_ quality: String) async { try? await playback.setQuality(quality: quality) }
    func setAudio(_ language: String) async { try? await playback.setAudioLanguage(language: language) }
    func setSubtitle(_ language: String) async { try? await playback.setSubtitleLanguage(language: language) }

    func signOut() {
        // Чистит токены общим слоем; `ObserveAuthStateUseCase` вернёт на онбординг.
        Task { _ = try? await logout.invoke() }
    }
}
