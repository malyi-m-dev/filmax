package com.filmax.data.fake.fixtures

import kotlin.test.Test
import kotlin.test.assertTrue

/** Инварианты фикстур, на которые опирается всё остальное: фейк каталога, ленты, детали. */
class FixtureCatalogTest {

    @Test
    fun `title ids are unique`() {
        // Дубль id тихо ломает выдачу: детали открывают не тот тайтл, а ленивые списки с key = id
        // падают на неуникальном ключе. Тайтлы дописываются руками, и номер легко взять занятый.
        val duplicates = buildFixtureCatalog().entries.groupBy { it.id }.filterValues { it.size > 1 }.keys

        assertTrue(duplicates.isEmpty(), "id заняты дважды: $duplicates")
    }
}
