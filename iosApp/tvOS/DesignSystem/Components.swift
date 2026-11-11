import SwiftUI
import Shared

// Компоненты Apple TV (аналог `core:tv-designsystem`): экраны собираются из них, а не из «сырых»
// View. Фокус, подъём карточки и звук — штатные tvOS (`.card`, `.bordered`): родной отклик пульта
// важнее своей анимации, а своя анимация фокуса на tvOS ещё и теряет параллакс.

// MARK: - Карточка

/// Форма карточки: постер 2:3 — тайтлы и подборки, широкая 16:9 — серии и «Продолжить».
enum TvCardShape {
    case poster, wide

    var width: CGFloat {
        switch self {
        case .poster: return TvMetrics.posterWidth
        case .wide: return TvMetrics.wideWidth
        }
    }

    var aspectRatio: CGFloat {
        switch self {
        case .poster: return posterAspectRatio
        case .wide: return TvMetrics.wideAspectRatio
        }
    }

    var height: CGFloat { width / aspectRatio }
}

/// Пункт контекстного меню карточки — долгое нажатие на пульте, как в приложениях Apple TV.
struct TvMenuAction: Identifiable {
    let title: String
    let systemImage: String
    var isDestructive = false
    let action: () -> Void

    var id: String { title }
}

/// Карточка-переход. Картинка — системная `.card`-кнопка (подъём, параллакс, блик в фокусе),
/// подписи — под ней, вне кнопки: так подложка карточки не растекается на текст, как в TV-приложении
/// Apple. Куда ведёт — `AppRoute`; какой это экран, знает только `tvAppDestinations`.
struct TvPosterCard: View {
    let route: AppRoute
    let posterURL: String?
    let title: String
    var subtitle: String? = nil
    var progress: Double? = nil
    var ratingText: String? = nil
    var shape: TvCardShape = .poster
    var placeholderIcon: String = "film"
    var menu: [TvMenuAction] = []

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.lg) {
            NavigationLink(value: route) { artwork }
                .buttonStyle(.card)
                .modifier(TvContextMenu(actions: menu))
            caption
        }
        .frame(width: shape.width, alignment: .leading)
    }

    private var artwork: some View {
        TvFillImage(url: posterURL, placeholderIcon: placeholderIcon)
            .frame(width: shape.width, height: shape.height)
            .overlay(alignment: .topTrailing) {
                if let ratingText { TvRatingBadge(text: ratingText).padding(DS.Spacing.sm) }
            }
            .overlay(alignment: .bottom) {
                if let progress { TvProgressBar(fraction: progress).padding(DS.Spacing.md) }
            }
            .clipShape(RoundedRectangle(cornerRadius: DS.Radius.poster))
    }

    private var caption: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.xs) {
            Text(title)
                .font(TvType.cardTitle)
                .foregroundColor(Theme.text)
                .lineLimit(1)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .font(TvType.caption)
                    .foregroundColor(Theme.onSurfaceVariant)
                    .lineLimit(1)
            }
        }
    }
}

extension TvPosterCard {
    /// Тайтл каталога → детали.
    init(item: Item, menu: [TvMenuAction] = []) {
        self.init(
            route: .details(itemId: item.id),
            posterURL: item.posterURL,
            title: item.title,
            subtitle: item.posterMeta,
            ratingText: item.ratingText,
            menu: menu
        )
    }

    /// Запись истории → сразу плеер с места остановки: ради этого её и открывают.
    init(history: WatchHistory, menu: [TvMenuAction] = []) {
        self.init(
            route: .player(itemId: history.itemId, target: history.target),
            posterURL: history.wideOrPoster,
            title: history.title,
            subtitle: history.subtitle,
            progress: history.progressFraction,
            shape: .wide,
            menu: menu
        )
    }

    /// Серия → плеер с сезоном и номером: номер уникален только внутри сезона.
    init(itemId: Int32, track: MediaTrack) {
        self.init(
            route: .player(itemId: itemId, target: track.target),
            posterURL: track.thumbnail,
            title: track.title.isEmpty ? track.episodeCode : track.title,
            subtitle: track.title.isEmpty ? nil : track.episodeCode,
            progress: track.progressFraction,
            shape: .wide,
            placeholderIcon: "play.rectangle"
        )
    }

    init(collection: FilmCollection) {
        self.init(
            route: .collection(id: collection.id, title: collection.title),
            posterURL: collection.posterURL,
            title: collection.title,
            placeholderIcon: "rectangle.stack"
        )
    }

    /// Запись «Буду смотреть» — лёгкая модель списка, а не полный тайтл.
    init(watchlistItem entry: WatchlistItem, menu: [TvMenuAction] = []) {
        self.init(
            route: .details(itemId: entry.id),
            posterURL: entry.posterSmall,
            title: entry.title,
            subtitle: entry.year > 0 ? "\(entry.year)" : nil,
            ratingText: ratingLabel(entry.rating?.doubleValue),
            menu: menu
        )
    }
}

/// Меню вешается только при наличии пунктов: долгое нажатие на карточке без действий не должно
/// открывать пустое меню.
private struct TvContextMenu: ViewModifier {
    let actions: [TvMenuAction]

    @ViewBuilder func body(content: Content) -> some View {
        if actions.isEmpty {
            content
        } else {
            content.contextMenu {
                ForEach(actions) { entry in
                    Button(role: entry.isDestructive ? .destructive : nil, action: entry.action) {
                        Label(entry.title, systemImage: entry.systemImage)
                    }
                }
            }
        }
    }
}

// MARK: - Картинки

/// Картинка по URL общего слоя с заполнением и обрезкой: кадр 16:9 и постер 2:3 приходят в одну
/// и ту же рамку (запасной постер у «Продолжить»), и растянуть их было бы хуже, чем обрезать.
struct TvFillImage: View {
    let url: String?
    var placeholderIcon: String = "film"

    var body: some View {
        AsyncImage(url: URL(string: url ?? "")) { phase in
            switch phase {
            case .success(let image):
                image.resizable().scaledToFill()
            default:
                placeholder
            }
        }
        // Та же ячейка с другим адресом (серии другого сезона) обязана загрузить новую картинку.
        .id(url)
    }

    private var placeholder: some View {
        ZStack {
            Theme.surface
            Image(systemName: placeholderIcon)
                .font(TvType.icon)
                .foregroundColor(Theme.onSurfaceVariant)
        }
    }
}

/// Широкий кадр под шапкой героя и деталей. Снизу и слева — затемнение в фон, чтобы текст поверх
/// читался на любом кадре. Рамку задаёт прозрачная подложка: так обрезанная картинка не раздвигает
/// ленту шире экрана.
struct TvBackdrop: View {
    let url: String?
    let height: CGFloat

    var body: some View {
        Color.clear
            .frame(height: height)
            .frame(maxWidth: .infinity)
            .overlay { TvFillImage(url: url) }
            .clipped()
            .overlay {
                LinearGradient(colors: [.clear, Theme.background], startPoint: .center, endPoint: .bottom)
            }
            .overlay {
                LinearGradient(
                    colors: [Theme.background.opacity(0.85), .clear],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            }
    }
}

// MARK: - Бейджи

private struct TvProgressBar: View {
    let fraction: Double

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(Color.black.opacity(0.5))
                Capsule().fill(Theme.accent)
                    .frame(width: proxy.size.width * CGFloat(min(max(fraction, 0), 1)))
            }
        }
        .frame(height: TvMetrics.progressHeight)
    }
}

/// Оценка на постере — вторым акцентом, как рейтинги во всей дизайн-системе.
struct TvRatingBadge: View {
    let text: String

    var body: some View {
        HStack(spacing: DS.Spacing.xs) {
            Image(systemName: "star.fill").foregroundColor(Theme.accent2)
            Text(text).fontWeight(.bold)
        }
        .font(TvType.caption)
        .foregroundColor(.white)
        .padding(.horizontal, DS.Spacing.sm)
        .padding(.vertical, DS.Spacing.xs)
        .background(.black.opacity(0.65))
        .clipShape(Capsule())
    }
}

// MARK: - Заголовки

struct TvSectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(TvType.section)
            .foregroundColor(Theme.text)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Заголовок открытого экрана (папка, подборка, фильмография) — в ленте, а не в навигационной
/// панели: на tvOS она занимает верх экрана и не прокручивается вместе с лентой.
struct TvScreenTitle: View {
    let title: String
    var subtitle: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.xs) {
            Text(title)
                .font(TvType.screenTitle)
                .foregroundColor(Theme.text)
                .lineLimit(2)
            if let subtitle {
                Text(subtitle)
                    .font(TvType.body)
                    .foregroundColor(Theme.onSurfaceVariant)
            }
        }
        .padding(.horizontal, DS.tvSafeHorizontal)
        .padding(.top, DS.Spacing.lg)
    }
}

/// Подпись над рядом выбора (фильтры, настройки).
struct TvFieldLabel: View {
    let title: String

    var body: some View {
        Text(title)
            .font(TvType.caption)
            .foregroundColor(Theme.textSecondary)
            .padding(.horizontal, DS.tvSafeHorizontal)
    }
}

// MARK: - Состояния экрана

struct TvLoadingView: View {
    var body: some View {
        ProgressView()
            .tint(Theme.accent)
            .scaleEffect(2)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.vertical, DS.Spacing.xxl)
    }
}

/// Ошибка экрана поверх `RequestResult.Error`: текст общего слоя и, если есть что повторить, кнопка.
struct TvErrorView: View {
    let message: String
    var systemImage: String = "exclamationmark.triangle.fill"
    var retry: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: DS.Spacing.lg) {
            Image(systemName: systemImage)
                .font(TvType.stateIcon)
                .foregroundColor(Theme.error)
            Text(message)
                .font(TvType.body)
                .foregroundColor(Theme.textSecondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: TvMetrics.formWidth)
            if let retry {
                Button(action: retry) { Label("Повторить", systemImage: "arrow.clockwise") }
                    .tvActionStyle(.primary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.vertical, DS.Spacing.xxl)
    }
}

struct TvEmptyStateView: View {
    let message: String
    var systemImage: String = "tray"

    var body: some View {
        VStack(spacing: DS.Spacing.lg) {
            Image(systemName: systemImage)
                .font(TvType.stateIcon)
                .foregroundColor(Theme.onSurfaceVariant)
            Text(message)
                .font(TvType.body)
                .foregroundColor(Theme.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .frame(maxWidth: TvMetrics.formWidth)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.vertical, DS.Spacing.xxl)
    }
}
