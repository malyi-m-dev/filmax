import SwiftUI

// Базовые SwiftUI-компоненты Filmax для iPhone/iPad (аналог `core:designsystem`/`core:ui`).
// Экраны собираются из них, а не из «сырых» View. TV-вариант — в `tvOS/DesignSystem`.

// MARK: - PosterCard

/// Карточка постера (2:3) с названием, опциональным подзаголовком, рейтингом и прогресс-полосой.
/// Универсальна: принимает примитивы, чтобы переиспользоваться для `Item`/`WatchHistory`/`Collection`.
struct PosterCard: View {
    let posterURL: String?
    let title: String
    var subtitle: String? = nil
    /// Доля просмотра 0..1 (для «продолжить смотреть»); `nil` — полоса не показывается.
    var progress: Double? = nil
    /// Готовый текст рейтинга (напр. "7.9"); `nil` — бейдж скрыт.
    var ratingText: String? = nil
    var width: CGFloat = 130

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.sm) {
            posterImage
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.white)
                .lineLimit(1)
            if let subtitle {
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundColor(Theme.onSurfaceVariant)
                    .lineLimit(1)
            }
        }
        .frame(width: width)
    }

    private var posterImage: some View {
        PosterImage(url: posterURL)
            .aspectRatio(posterAspectRatio, contentMode: .fill)
            .frame(width: width, height: width / posterAspectRatio)
            .clipShape(RoundedRectangle(cornerRadius: DS.Radius.poster))
            .overlay(alignment: .topTrailing) {
                if let ratingText {
                    RatingBadge(text: ratingText).padding(DS.Spacing.xs)
                }
            }
            .overlay(alignment: .bottom) {
                if let progress {
                    ProgressBar(fraction: progress)
                        .padding(.horizontal, DS.Spacing.xs)
                        .padding(.bottom, DS.Spacing.xs)
                }
            }
    }
}

private struct ProgressBar: View {
    let fraction: Double
    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Color.black.opacity(0.5))
                Capsule().fill(Theme.accent)
                    .frame(width: geo.size.width * CGFloat(min(max(fraction, 0), 1)))
            }
        }
        .frame(height: 4)
    }
}

// MARK: - RatingBadge

/// Бейдж рейтинга поверх постера.
struct RatingBadge: View {
    let text: String
    var body: some View {
        HStack(spacing: 2) {
            Image(systemName: "star.fill").font(.system(size: 9))
            Text(text).font(.system(size: 11, weight: .bold))
        }
        .foregroundColor(.white)
        .padding(.horizontal, 6)
        .padding(.vertical, 3)
        .background(.black.opacity(0.65))
        .clipShape(Capsule())
    }
}

// MARK: - FilmaxButton

/// Основная/второстепенная кнопка Filmax с акцентом дизайн-системы.
struct FilmaxButton: View {
    enum Style { case primary, secondary }
    let title: String
    var systemImage: String? = nil
    var style: Style = .primary
    let action: () -> Void

    var body: some View {
        let label = HStack(spacing: DS.Spacing.sm) {
            if let systemImage { Image(systemName: systemImage) }
            Text(title).fontWeight(.semibold)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 4)

        switch style {
        case .primary:
            Button(action: action) { label }
                .buttonStyle(.borderedProminent).tint(Theme.accent).controlSize(.large)
        case .secondary:
            Button(action: action) { label }
                .buttonStyle(.bordered).tint(Theme.accent).controlSize(.large)
        }
    }
}

// MARK: - SectionHeader

/// Заголовок секции/рельсы с опциональной кнопкой «Все».
struct SectionHeader: View {
    let title: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.white)
            Spacer()
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .font(.system(size: 14, weight: .semibold))
                    .tint(Theme.accent)
            }
        }
    }
}

// MARK: - Состояния экрана

/// Индикатор загрузки на весь экран.
struct LoadingView: View {
    var body: some View {
        ProgressView()
            .tint(Theme.accent)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// Единый компонент ошибки поверх `RequestResult.Error`/`AppError` (§5 доки).
/// Показывает сообщение из общего слоя и кнопку «Повторить».
struct ErrorView: View {
    let message: String
    var systemImage: String = "exclamationmark.triangle.fill"
    var retry: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: DS.Spacing.md) {
            Image(systemName: systemImage)
                .font(.system(size: 44))
                .foregroundColor(Theme.accent)
            Text(message)
                .font(.system(size: 15))
                .foregroundColor(Theme.onSurfaceVariant)
                .multilineTextAlignment(.center)
            if let retry {
                Button("Повторить", action: retry)
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
            }
        }
        .padding(DS.Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// Пустое состояние (нет данных) — переиспользуется списками/сетками.
struct EmptyStateView: View {
    let message: String
    var systemImage: String = "tray"
    var body: some View {
        VStack(spacing: DS.Spacing.md) {
            Image(systemName: systemImage)
                .font(.system(size: 40))
                .foregroundColor(Theme.onSurfaceVariant)
            Text(message)
                .font(.system(size: 15))
                .foregroundColor(Theme.onSurfaceVariant)
                .multilineTextAlignment(.center)
        }
        .padding(DS.Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// Ненавязчивый баннер «нет сети» (для офлайн-деградации, перекликается с бэкенд-задачей #42).
struct OfflineBanner: View {
    var body: some View {
        HStack(spacing: DS.Spacing.sm) {
            Image(systemName: "wifi.slash")
            Text("Нет сети — показаны сохранённые данные")
                .font(.system(size: 13, weight: .medium))
        }
        .foregroundColor(.white)
        .padding(.horizontal, DS.Spacing.md)
        .padding(.vertical, DS.Spacing.sm)
        .frame(maxWidth: .infinity)
        .background(Theme.accentSoft)
    }
}
