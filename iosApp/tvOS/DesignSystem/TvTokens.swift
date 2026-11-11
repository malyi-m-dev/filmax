import SwiftUI

/// Шрифты Apple TV (аналог `FxType` на Android TV). Экран смотрят с дивана, поэтому шкала крупнее
/// телефонной; заголовки и подписи кнопок плотнее, текст — обычного веса.
enum TvType {
    static let hero = Font.system(size: 64, weight: .heavy)
    static let screenTitle = Font.system(size: 48, weight: .bold)
    static let section = Font.system(size: 34, weight: .bold)
    static let cardTitle = Font.system(size: 24, weight: .semibold)
    static let body = Font.system(size: 26)
    static let caption = Font.system(size: 22)
    /// Код входа читают с другого конца комнаты и набирают на телефоне — моноширинный и крупный.
    static let code = Font.system(size: 96, weight: .heavy, design: .monospaced)
    static let icon = Font.system(size: 72)
    static let stateIcon = Font.system(size: 90)
}

/// Размеры Apple TV (аналог `TvMetrics` на Android TV): карточки, шапки, колонки текста.
enum TvMetrics {
    /// Постер 2:3 в рядах и сетках — шесть в строку на 1080p при полях `DS.tvSafeHorizontal`.
    static let posterWidth: CGFloat = 230
    /// Широкая карточка 16:9 — «Продолжить», серии, история: кадр серии точнее постера.
    static let wideWidth: CGFloat = 380
    static let wideAspectRatio: CGFloat = 16.0 / 9.0
    static let heroHeight: CGFloat = 620
    static let detailsBackdropHeight: CGFloat = 680
    /// Запас под подъём карточки в фокусе: ScrollView обрезает всё, что вышло за его край.
    static let focusLift: CGFloat = DS.Spacing.xl
    /// Ширина колонки описания: строка на весь экран телевизора не читается.
    static let textColumnWidth: CGFloat = 1100
    /// Ширина формы входа и списков в модальных окнах.
    static let formWidth: CGFloat = 900
    static let sourceCardWidth: CGFloat = 520
    static let sourceCardHeight: CGFloat = 340
    static let codeCardWidth: CGFloat = 620
    static let avatarSize: CGFloat = 180
    static let columnGap: CGFloat = 96
    static let progressHeight: CGFloat = 6
    static let stepBadgeSize: CGFloat = 56
    static let outlineWidth: CGFloat = 2
    /// Разрядка подписей капсом: без неё «КОД АКТИВАЦИИ» сливается в полосу.
    static let captionTracking: CGFloat = 3
}
