import SwiftUI

// Компоненты дизайн-системы для Apple TV (аналог `core:tv-designsystem`).
// Отличия от iOS: focus engine, крупные размеры, TV-safe отступы, пультовая навигация.

// MARK: - Focus-стиль карточки

/// Стиль кнопки-карточки для tvOS: подсветка и увеличение при фокусе (пультовая навигация).
struct TvCardButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        FocusableCard(configuration: configuration)
    }

    private struct FocusableCard: View {
        let configuration: ButtonStyleConfiguration
        @Environment(\.isFocused) private var focused

        var body: some View {
            configuration.label
                .scaleEffect(focused ? 1.12 : 1.0)
                .shadow(color: .black.opacity(focused ? 0.6 : 0), radius: focused ? 24 : 0, y: 12)
                .animation(.easeOut(duration: 0.15), value: focused)
        }
    }
}

// MARK: - TvPosterCard

/// Крупная фокус-карточка постера для Apple TV. `NavigationLink(value:)` → пультовый push
/// на зарегистрированный `AppRoute` (см. `tvAppDestinations`); фокусом управляет движок.
struct TvPosterCard: View {
    let route: AppRoute
    let posterURL: String?
    let title: String
    var subtitle: String? = nil
    var progress: Double? = nil
    var ratingText: String? = nil
    var width: CGFloat = 240

    var body: some View {
        NavigationLink(value: route) {
            VStack(alignment: .leading, spacing: DS.Spacing.sm) {
                PosterImage(url: posterURL)
                    .aspectRatio(posterAspectRatio, contentMode: .fill)
                    .frame(width: width, height: width / posterAspectRatio)
                    .clipShape(RoundedRectangle(cornerRadius: DS.Radius.lg))
                    .overlay(alignment: .topTrailing) {
                        if let ratingText { TvRatingBadge(text: ratingText).padding(DS.Spacing.sm) }
                    }
                    .overlay(alignment: .bottom) {
                        if let progress { TvProgressBar(fraction: progress).padding(DS.Spacing.sm) }
                    }
                Text(title)
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundColor(.white)
                    .lineLimit(1)
                if let subtitle {
                    Text(subtitle)
                        .font(.system(size: 18))
                        .foregroundColor(Theme.onSurfaceVariant)
                        .lineLimit(1)
                }
            }
            .frame(width: width)
        }
        .buttonStyle(TvCardButtonStyle())
    }
}

private struct TvProgressBar: View {
    let fraction: Double
    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Color.black.opacity(0.5))
                Capsule().fill(Theme.accent)
                    .frame(width: geo.size.width * CGFloat(min(max(fraction, 0), 1)))
            }
        }
        .frame(height: 6)
    }
}

// MARK: - TvRatingBadge

struct TvRatingBadge: View {
    let text: String
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "star.fill").font(.system(size: 14))
            Text(text).font(.system(size: 16, weight: .bold))
        }
        .foregroundColor(.white)
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(.black.opacity(0.65))
        .clipShape(Capsule())
    }
}

// MARK: - TvSectionHeader

struct TvSectionHeader: View {
    let title: String
    var body: some View {
        Text(title)
            .font(.system(size: 32, weight: .bold))
            .foregroundColor(.white)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Состояния экрана (TV)

struct TvLoadingView: View {
    var body: some View {
        ProgressView()
            .tint(Theme.accent)
            .scaleEffect(2)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// Единый компонент ошибки для Apple TV поверх `RequestResult.Error`/`AppError` (§5 доки).
struct TvErrorView: View {
    let message: String
    var systemImage: String = "exclamationmark.triangle.fill"
    var retry: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: DS.Spacing.lg) {
            Image(systemName: systemImage)
                .font(.system(size: 90))
                .foregroundColor(Theme.accent)
            Text(message)
                .font(.system(size: 30))
                .foregroundColor(Theme.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 900)
            if let retry {
                Button("Повторить", action: retry)
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct TvEmptyStateView: View {
    let message: String
    var systemImage: String = "tray"
    var body: some View {
        VStack(spacing: DS.Spacing.lg) {
            Image(systemName: systemImage)
                .font(.system(size: 80))
                .foregroundColor(Theme.onSurfaceVariant)
            Text(message)
                .font(.system(size: 30))
                .foregroundColor(Theme.onSurfaceVariant)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
