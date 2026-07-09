import Combine
import Shared

/// Презентейшен Главной (iOS + tvOS): грузит агрегированную ленту общим `GetHomeFeedUseCase`.
/// Бизнес-логика (параллельные запросы, сборка `HomeFeed`) — в KMP; здесь только состояние экрана.
@MainActor
final class HomeViewModel: ObservableObject {
    @Published var feed: HomeFeed?
    @Published var isLoading = false
    /// Жёсткая ошибка (пусто + сбой) — показываем `ErrorView`. Мягкая (`HomeFeed.error`) — баннером.
    @Published var error: String?

    private let getHomeFeed = UseCaseProvider.shared.getHomeFeedUseCase()

    func load() async {
        isLoading = true
        error = nil
        do {
            let loaded = try await getHomeFeed.invoke()
            feed = loaded
            if loaded.isEmptyFeed {
                error = loaded.error ?? "Не удалось загрузить ленту."
            }
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

private extension HomeFeed {
    /// Лента считается пустой, если ни одна секция не пришла (кандидат на экран ошибки).
    var isEmptyFeed: Bool {
        hero == nil
            && continueWatching.isEmpty
            && collections.isEmpty
            && trending.isEmpty
            && forYou.isEmpty
    }
}
