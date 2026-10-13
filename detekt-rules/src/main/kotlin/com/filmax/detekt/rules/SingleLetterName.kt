package com.filmax.detekt.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Запрещает однобуквенные имена: параметров (функций, лямбд, `catch`, `for`), переменных и частей
 * деструктуризации. В `s.color` не видно, что такое `s`, — имя называет смысл значения, а для
 * единственного параметра короткой лямбды есть `it`.
 *
 * Не флагуются `_` (это отказ от значения, а не имя) и параметры типов (`T`, `K`, `V` — конвенция
 * языка). Обрезанные имена вроде `dl` правило не ловит: отличить сокращение от слова (`id`, `ui`)
 * механически нельзя — это остаётся ревью.
 */
class SingleLetterName(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        id = "SingleLetterName",
        severity = Severity.Style,
        description = "Однобуквенное имя не говорит, что в нём лежит: назовите по смыслу " +
            "или используйте `it`.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitParameter(parameter: KtParameter) {
        super.visitParameter(parameter)
        // У деструктуризации `{ (a, b) -> }` своё имя у каждой части — их проверяет
        // visitDestructuringDeclarationEntry, а у самого параметра имени нет.
        if (parameter.destructuringDeclaration == null) check(parameter)
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        check(property)
    }

    override fun visitDestructuringDeclarationEntry(multiDeclarationEntry: KtDestructuringDeclarationEntry) {
        super.visitDestructuringDeclarationEntry(multiDeclarationEntry)
        check(multiDeclarationEntry)
    }

    private fun check(declaration: KtNamedDeclaration) {
        val name = declaration.name ?: return
        if (name.length != 1 || name == "_") return
        report(CodeSmell(issue, Entity.atName(declaration), "«$name»: ${issue.description}"))
    }
}
