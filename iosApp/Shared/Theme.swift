import SwiftUI

/// Цвета Filmax — палитра «Фиолет» общей дизайн-системы (`FilmaxPalettes.Violet` на Android):
/// акцент — главное действие, текущий раздел и прогресс; второй акцент — рейтинги. Поверхности —
/// полупрозрачный белый поверх тёмного фона, как стекло на Android.
enum Theme {
    static let accent = Color(red: 0x7B / 255, green: 0x5C / 255, blue: 0xF5 / 255)
    static let accent2 = Color(red: 0xF5 / 255, green: 0xC4 / 255, blue: 0x4C / 255)
    static let background = Color(red: 0x0F / 255, green: 0x0C / 255, blue: 0x1A / 255)
    static let surface = Color.white.opacity(0.07)
    static let surfaceHigh = Color.white.opacity(0.12)
    static let text = Color(red: 0xF4 / 255, green: 0xF2 / 255, blue: 0xFA / 255)
    static let textSecondary = text.opacity(0.72)
    static let onSurfaceVariant = text.opacity(0.5)
    /// Ошибки и деструктив — приглушённый красный, общий для всех палитр.
    static let error = Color(red: 0xF0 / 255, green: 0x7A / 255, blue: 0x6A / 255)
    /// Совместимость с ранними экранами: прежний «мягкий» акцент.
    static let accentSoft = accent.opacity(0.6)
}
