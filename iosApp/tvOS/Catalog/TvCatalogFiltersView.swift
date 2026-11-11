import SwiftUI
import Shared

/// Фильтры каталога (модальное окно): годы, пороги оценок источника, страна, «Только 4K» и
/// «Только завершённые». Правится черновик — в каталог он уходит только по «Показать», а «Меню» на
/// пульте закрывает окно без изменений. Набор полей — по `SourceCapabilities`: чего источник не
/// умеет, того и не предлагаем.
struct TvCatalogFiltersView: View {
    let capabilities: SourceCapabilities
    let countries: [Country]
    let onApply: (CatalogFilters) -> Void

    @State private var draft: CatalogFilters
    @Environment(\.dismiss) private var dismiss

    /// Границы десятилетий: год пультом не набрать, а десятилетие — один щелчок.
    private static let decadeStarts: [Int32] = [1960, 1970, 1980, 1990, 2000, 2010, 2020]
    private static let ratingSteps: [Int32] = [5, 6, 7, 8, 9]
    private static let yearsInDecade: Int32 = 9

    init(
        filters: CatalogFilters,
        capabilities: SourceCapabilities,
        countries: [Country],
        onApply: @escaping (CatalogFilters) -> Void
    ) {
        self.capabilities = capabilities
        self.countries = countries
        self.onApply = onApply
        _draft = State(initialValue: filters)
    }

    var body: some View {
        TvScreenScroll {
            TvScreenTitle(title: "Фильтры")
            yearSection
            ForEach(capabilities.ratings, id: \.self) { source in
                ratingSection(source)
            }
            if capabilities.countries && !countries.isEmpty {
                countrySection
            }
            switches
            actions
        }
    }

    // MARK: Годы

    private var yearSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            TvFieldLabel(title: "Год выхода — с")
            TvChipRow(options: Self.yearFromOptions, selected: draft.yearFrom?.int32Value) { year in
                setYearFrom(year)
            }
            TvFieldLabel(title: "по")
            TvChipRow(options: Self.yearToOptions, selected: draft.yearTo?.int32Value) { year in
                setYearTo(year)
            }
        }
    }

    private static var yearFromOptions: [TvChipOption<Int32?>] {
        [TvChipOption<Int32?>(value: nil, title: "Любой")]
            + decadeStarts.map { TvChipOption<Int32?>(value: $0, title: "\($0)") }
    }

    /// Конец последнего десятилетия не нужен: «по» без границы — это «Любой».
    private static var yearToOptions: [TvChipOption<Int32?>] {
        let decadeEnds = decadeStarts.dropLast().map { start in start + yearsInDecade }
        return [TvChipOption<Int32?>(value: nil, title: "Любой")]
            + decadeEnds.map { TvChipOption<Int32?>(value: $0, title: "\($0)") }
    }

    /// Нижняя граница выше верхней дала бы пустую выдачу — противоречащая граница снимается.
    private func setYearFrom(_ year: Int32?) {
        var upper = draft.yearTo?.int32Value
        if let year, let current = upper, current < year { upper = nil }
        draft = CatalogKit.shared.withYears(filters: draft, from: year.kotlin, to: upper.kotlin)
    }

    private func setYearTo(_ year: Int32?) {
        var lower = draft.yearFrom?.int32Value
        if let year, let current = lower, current > year { lower = nil }
        draft = CatalogKit.shared.withYears(filters: draft, from: lower.kotlin, to: year.kotlin)
    }

    // MARK: Оценки и страна

    private func ratingSection(_ source: RatingSource) -> some View {
        let options = [TvChipOption<Int32?>(value: nil, title: "Любая")]
            + Self.ratingSteps.map { TvChipOption<Int32?>(value: $0, title: "от \($0)") }
        let current = CatalogKit.shared.minRating(filters: draft, source: source)?.int32Value
        return VStack(alignment: .leading, spacing: 0) {
            TvFieldLabel(title: "Оценка: \(source.title)")
            TvChipRow(options: options, selected: current) { value in
                draft = CatalogKit.shared.withMinRating(filters: draft, source: source, value: value.kotlin)
            }
        }
    }

    private var countrySection: some View {
        VStack(alignment: .leading, spacing: 0) {
            TvFieldLabel(title: "Страна")
            TvChipRow(options: countryOptions, selected: draft.countryId?.int32Value) { countryId in
                draft = CatalogKit.shared.withCountry(filters: draft, countryId: countryId.kotlin)
            }
        }
    }

    private var countryOptions: [TvChipOption<Int32?>] {
        [TvChipOption<Int32?>(value: nil, title: "Любая")]
            + countries.map { TvChipOption<Int32?>(value: $0.id, title: $0.title) }
    }

    // MARK: Переключатели и действия

    /// Переключатели — чипы с галочкой: системный `Toggle` на tvOS вне списка выглядит чужеродно.
    private var switches: some View {
        HStack(spacing: DS.Spacing.md) {
            TvChip(title: "Только 4K", isSelected: draft.only4k) {
                draft = CatalogKit.shared.withOnly4k(filters: draft, only4k: !draft.only4k)
            }
            // Домен различает «только идущие» (false), но на ТВ, как и на телефоне, — пара «все / завершённые».
            TvChip(title: "Только завершённые", isSelected: onlyFinished) {
                draft = CatalogKit.shared.withFinished(filters: draft, finished: onlyFinished ? nil : KotlinBoolean(bool: true))
            }
        }
        .padding(.horizontal, DS.tvSafeHorizontal)
        .padding(.vertical, DS.Spacing.lg)
        .focusSection()
    }

    private var onlyFinished: Bool { draft.onlyFinished?.boolValue == true }

    private var actions: some View {
        HStack(spacing: DS.Spacing.lg) {
            Button {
                onApply(draft)
                dismiss()
            } label: {
                Label("Показать", systemImage: "checkmark")
            }
            .tvActionStyle(.primary)
            Button {
                draft = CatalogKit.shared.noFilters
            } label: {
                Label("Сбросить", systemImage: "arrow.counterclockwise")
            }
            .tvActionStyle(.secondary)
        }
        .padding(.horizontal, DS.tvSafeHorizontal)
        .padding(.top, DS.Spacing.lg)
        .focusSection()
    }
}
