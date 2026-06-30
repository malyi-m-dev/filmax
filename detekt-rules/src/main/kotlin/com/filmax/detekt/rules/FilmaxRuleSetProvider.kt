package com.filmax.detekt.rules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/**
 * Набор кастомных правил Filmax. Id `filmax-rules` совпадает с секцией в
 * config/detekt/detekt.yml. Регистрируется через файл сервиса в META-INF/services.
 */
class FilmaxRuleSetProvider : RuleSetProvider {

    override val ruleSetId = "filmax-rules"

    override fun instance(config: Config): RuleSet =
        RuleSet(ruleSetId, listOf(NestedIf(config)))
}
