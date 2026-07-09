import SwiftUI

/// Токены дизайн-системы Filmax, общие для iOS и tvOS (аналог `core:designsystem` на Android).
/// Цвета берутся из `Theme.swift`; здесь — отступы, радиусы и типографика.
/// Размеры для TV крупнее — платформенные компоненты (`tvOS/DesignSystem`) используют `.tv`-значения.
enum DS {
    /// Единый шаг отступов (кратный 4) — согласован с Android-дизайн-системой.
    enum Spacing {
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let xl: CGFloat = 32
        static let xxl: CGFloat = 48
    }

    /// Радиусы скругления карточек/кнопок.
    enum Radius {
        static let sm: CGFloat = 8
        static let md: CGFloat = 14
        static let lg: CGFloat = 20
        static let xl: CGFloat = 28
        static let poster: CGFloat = 12
    }

    /// TV-safe отступ до краёв экрана (overscan) для Apple TV-раскладок.
    static let tvSafeHorizontal: CGFloat = 90
    static let tvSafeVertical: CGFloat = 60
}

/// Соотношение сторон постера (2:3) — единое для сеток и рельс на обеих платформах.
let posterAspectRatio: CGFloat = 2.0 / 3.0
