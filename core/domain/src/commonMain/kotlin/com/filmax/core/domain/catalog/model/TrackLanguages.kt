package com.filmax.core.domain.catalog.model

/**
 * Названия языков дорожек kino.pub для людей. API отдаёт коды разного вида («rus», «ru»), а
 * экраны — плеер, «О фильме» в деталях — показывают одно и то же название.
 */
fun audioLanguageName(code: String?): String = languageName(code) ?: "Оригинал"

/** Язык субтитров; без кода — просто «Субтитры». */
fun subtitleLanguageName(code: String?): String = languageName(code) ?: "Субтитры"

private fun languageName(code: String?): String? = when (code?.lowercase()) {
    "rus", "ru" -> "Русский"
    "eng", "en" -> "English"
    "ukr", "uk" -> "Українська"
    null, "" -> null
    else -> code
}
