package com.filmax.detekt.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression

/**
 * Запрещает вложенный `if` внутри `if`. Код следует выпрямлять через ранний
 * `return` / guard-clause / `when` (см. docs/REFACTORING_PLAN.md).
 *
 * Цепочки `else if` НЕ считаются вложенностью: вложенный `if` ищется только в
 * `then`-ветке (и условии) внешнего `if`, а `else if` живёт в `else`-ветке —
 * поэтому не флагуется. Каждый уровень вложенности репортится один раз: глубокие
 * случаи находит отдельный визит самого вложенного `if`.
 */
class NestedIf(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        id = "NestedIf",
        severity = Severity.Maintainability,
        description = "Вложенный `if` внутри `if` усложняет чтение — выпрямите через " +
            "ранний return / guard-clause / when.",
        debt = Debt.TEN_MINS,
    )

    override fun visitIfExpression(expression: KtIfExpression) {
        super.visitIfExpression(expression)

        val nestedIfs = buildList {
            addAll(expression.then.directNestedIfs())
            (expression.condition as? KtIfExpression)?.let { add(it) }
        }
        nestedIfs.forEach { nested ->
            report(CodeSmell(issue, Entity.from(nested), issue.description))
        }
    }

    /**
     * Прямо вложенные `if` в ветке: либо `if` без скобок (`if (a) if (b) ...`),
     * либо верхнеуровневые операторы блока `{ ... }`.
     */
    private fun KtExpression?.directNestedIfs(): List<KtIfExpression> = when (this) {
        is KtIfExpression -> listOf(this)
        is KtBlockExpression -> statements.filterIsInstance<KtIfExpression>()
        else -> emptyList()
    }
}
