import SwiftUI
import Shared

// Раскладка экранов Apple TV: лента, ряды, сетки, чипы и кнопки действий. Поля до края экрана
// задаёт само содержимое (`DS.tvSafeHorizontal`), поэтому ряды прокручиваются под край телевизора,
// а не обрываются на системном отступе.

// MARK: - Лента экрана

/// Вертикальная лента экрана во всю ширину: системный боковой отступ снят, его роль играют поля
/// рядов. Иначе отступы складывались бы, а прокрученные ряды обрезались посреди экрана.
struct TvScreenScroll<Content: View>: View {
    private let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: DS.Spacing.xl) {
                content
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.bottom, DS.tvSafeVertical)
        }
        .ignoresSafeArea(.container, edges: .horizontal)
        .background(Theme.background.ignoresSafeArea())
    }
}

// MARK: - Ряд

/// Ряд карточек с заголовком. Ленивый: догрузка по последней карточке срабатывает на подходе к ней,
/// а не сразу для всех. `focusSection` — чтобы пульт «вниз» попадал в ряд, даже когда прямо под
/// фокусом карточки нет (короткий ряд под длинным).
struct TvRail<Content: View>: View {
    private let title: String
    private let allRoute: AppRoute?
    private let content: Content

    init(_ title: String, allRoute: AppRoute? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.allRoute = allRoute
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header.padding(.horizontal, DS.tvSafeHorizontal)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: DS.Spacing.xl) {
                    content
                }
                .padding(.horizontal, DS.tvSafeHorizontal)
                .padding(.vertical, TvMetrics.focusLift)
            }
            .focusSection()
        }
    }

    private var header: some View {
        HStack(spacing: DS.Spacing.lg) {
            Text(title)
                .font(TvType.section)
                .foregroundColor(Theme.text)
            if let allRoute {
                NavigationLink(value: allRoute) { Text("Все") }
                    .tvActionStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
    }
}

// MARK: - Сетка

/// Сетка карточек: колонки по ширине карточки, поля — TV-safe.
struct TvGrid<Content: View>: View {
    private let shape: TvCardShape
    private let content: Content

    init(shape: TvCardShape = .poster, @ViewBuilder content: () -> Content) {
        self.shape = shape
        self.content = content()
    }

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: shape.width), spacing: DS.Spacing.xl, alignment: .top)],
            alignment: .leading,
            spacing: DS.Spacing.xxl
        ) {
            content
        }
        .padding(.horizontal, DS.tvSafeHorizontal)
        .padding(.top, TvMetrics.focusLift)
    }
}

extension Array where Element == Item {
    /// Без повторов id: в ленивой сетке дубль ключа путает ячейки и фокус. Нужен там, где модель
    /// склеивает страницы, не убирая пересечений.
    func uniqueByID() -> [Item] {
        var seen = Set<Int32>()
        return filter { seen.insert($0.id).inserted }
    }
}

// MARK: - Кнопки действий

/// Роль кнопки: главное действие — акцентом, остальные — системной стеклянной кнопкой.
enum TvActionRole {
    case primary, secondary
}

extension View {
    /// Системные tvOS-кнопки: фокус, подъём и звук штатные. Роль кнопки постоянна — стиль по смене
    /// состояния менять нельзя: смена типа стиля пересоздаёт кнопку, и фокус с неё слетает.
    @ViewBuilder func tvActionStyle(_ role: TvActionRole) -> some View {
        switch role {
        case .primary:
            buttonStyle(.borderedProminent).tint(Theme.accent)
        case .secondary:
            buttonStyle(.bordered)
        }
    }
}

// MARK: - Чипы

/// Вариант выбора в ряду чипов.
struct TvChipOption<Value: Hashable> {
    let value: Value
    let title: String
}

/// Чип выбора. Выбранный отмечен галочкой и акцентным оттенком, но стиль кнопки у всех чипов один:
/// иначе выбор пересоздавал бы кнопку под фокусом и пульт терял место (см. `tvActionStyle`).
struct TvChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: DS.Spacing.sm) {
                if isSelected { Image(systemName: "checkmark") }
                Text(title)
            }
        }
        .buttonStyle(.bordered)
        .tint(isSelected ? Theme.accent : nil)
    }
}

/// Горизонтальный ряд чипов с одним выбранным значением.
struct TvChipRow<Value: Hashable>: View {
    let options: [TvChipOption<Value>]
    let selected: Value
    let onSelect: (Value) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DS.Spacing.md) {
                ForEach(options, id: \.value) { option in
                    TvChip(title: option.title, isSelected: option.value == selected) {
                        onSelect(option.value)
                    }
                }
            }
            .padding(.horizontal, DS.tvSafeHorizontal)
            .padding(.vertical, DS.Spacing.lg)
        }
        .focusSection()
    }
}
