package com.filmax.data.fake.fixtures

import com.filmax.core.domain.catalog.model.ItemType

/**
 * Люди из титров — выдуманные. Актёры кочуют из тайтла в тайтл, чтобы фильмография была не из
 * одной карточки: Воронцова, Северин, Велехов и Колдуэлл играют в четырёх-пяти тайтлах.
 */
internal object People {
    // Режиссёры
    val SAMARIN = Person(id = 1, name = "Андрей Самарин")
    val BELETSKAYA = Person(id = 2, name = "Ольга Белецкая")
    val ABDULLAEV = Person(id = 3, name = "Тимур Абдуллаев")
    val LANSKOY = Person(id = 4, name = "Кирилл Ланской")
    val OSTROVSKAYA = Person(id = 5, name = "Мария Островская")
    val CHISTYAKOV = Person(id = 6, name = "Глеб Чистяков")
    val HARTWELL = Person(id = 7, name = "Дэниел Хартвелл")
    val ROCKWOOD = Person(id = 8, name = "Эмили Роквуд")
    val MATSUDA = Person(id = 9, name = "Хироси Мацуда")
    val CHOI = Person(id = 10, name = "Чхве Ын Соль")
    val MERCIER = Person(id = 11, name = "Жан-Луи Мерсье")
    val LINDQVIST = Person(id = 12, name = "Свен Линдквист")
    val KORSAKOVA = Person(id = 13, name = "Нина Корсакова")
    val VOLSKY = Person(id = 14, name = "Арсений Вольский")
    val BRANDT = Person(id = 15, name = "Ханна Брандт")
    val ORTEGA = Person(id = 16, name = "Лусия Ортега")
    val COLFIELD = Person(id = 17, name = "Брайан Колфилд")
    val OHASHI = Person(id = 18, name = "Кэнта Охаси")
    val PEYTON = Person(id = 19, name = "Эндрю Пейтон")
    val MORGAN = Person(id = 20, name = "Рэйчел Морган")
    val SAPAROVA = Person(id = 21, name = "Айгерим Сапарова")

    // Актёры и рассказчики
    val VORONTSOVA = Person(id = 31, name = "Анна Воронцова")
    val SEVERIN = Person(id = 32, name = "Илья Северин")
    val LESNITSKAYA = Person(id = 33, name = "Дарья Лесницкая")
    val GORDEEV = Person(id = 34, name = "Павел Гордеев")
    val ZOTOV = Person(id = 35, name = "Марк Зотов")
    val SABUROVA = Person(id = 36, name = "Вера Сабурова")
    val KRAYNEV = Person(id = 37, name = "Станислав Крайнев")
    val DOROKHOVA = Person(id = 38, name = "Полина Дорохова")
    val RYABININ = Person(id = 39, name = "Олег Рябинин")
    val LAVROVA = Person(id = 40, name = "Ксения Лаврова")
    val SHATALOV = Person(id = 41, name = "Тимофей Шаталов")
    val VENEDIKTOVA = Person(id = 42, name = "Алиса Венедиктова")
    val STUPIN = Person(id = 43, name = "Григорий Ступин")
    val MARTYNYUK = Person(id = 44, name = "Елена Мартынюк")
    val KEDROV = Person(id = 45, name = "Роман Кедров")
    val ERMOLAEVA = Person(id = 46, name = "Софья Ермолаева")
    val VELEKHOV = Person(id = 47, name = "Артём Велехов")
    val GRINEVSKAYA = Person(id = 48, name = "Наталья Гриневская")
    val PASECHNIK = Person(id = 49, name = "Леонид Пасечник")
    val CALDWELL = Person(id = 50, name = "Джеймс Колдуэлл")
    val STONE = Person(id = 51, name = "Эбигейл Стоун")
    val REILLY = Person(id = 52, name = "Томас Рейли")
    val DONOVAN = Person(id = 53, name = "Клэр Донован")
    val BRANDON = Person(id = 54, name = "Майкл Брэндон")
    val MOREAU = Person(id = 55, name = "Изабель Моро")
    val BERANGER = Person(id = 56, name = "Луи Беранже")
    val LEE_MIN_SO = Person(id = 57, name = "Ли Мин Со")
    val KANG_HYE_RIN = Person(id = 58, name = "Кан Хе Рин")
    val YOON_SO_JIN = Person(id = 59, name = "Юн Со Джин")
    val TANABE = Person(id = 60, name = "Юки Танабэ")
    val ASAHINA = Person(id = 61, name = "Рё Асахина")
    val KISARAGI = Person(id = 62, name = "Аюми Кисараги")
    val SANDBERG = Person(id = 63, name = "Эрик Сандберг")
    val HOLM = Person(id = 64, name = "Фрида Хольм")
    val VOGEL = Person(id = 65, name = "Лена Фогель")
    val KEHL = Person(id = 66, name = "Маттиас Кель")
    val MENDOZA = Person(id = 67, name = "Карлос Мендоса")
    val VARGAS = Person(id = 68, name = "Инес Варгас")
    val ALDRIDGE = Person(id = 69, name = "Маргарет Олдридж")
    val VALDAEV = Person(id = 70, name = "Игорь Валдаев")
    val AKHMETOV = Person(id = 71, name = "Данияр Ахметов")

    // Ведущие шоу и музыканты
    val STRELTSOV = Person(id = 72, name = "Егор Стрельцов")
    val BRIGHT = Person(id = 73, name = "Оливия Брайт")
    val ROSTOV = Person(id = 74, name = "Михаил Ростов")
    val ZVONAREVA = Person(id = 75, name = "Алина Звонарёва")
}

/**
 * Роль, которая у формата одна на всех: у документального кино — рассказчик, у шоу — ведущий, на
 * концерте — исполнитель. Профессия из [ROLES] под фото музыканта читалась бы нелепо.
 */
internal val FORMAT_ROLES: Map<ItemType, String> = mapOf(
    ItemType.DOCUMENTARY to "Рассказчик",
    ItemType.DOCUMENTARY_MOVIE to "Рассказчик",
    ItemType.TV_SHOW to "Ведущий",
    ItemType.CONCERT to "Исполнитель",
)

/**
 * Роли главного состава. Профессии, а не имена персонажей: подходят любому актёру, а экрану
 * важна сама подпись под фото, а не её смысл.
 */
internal val ROLES: List<String> = listOf(
    "Следователь",
    "Капитан",
    "Врач",
    "Журналист",
    "Адвокат",
    "Инженер",
    "Пилот",
    "Учитель",
    "Программист",
    "Фотограф",
    "Механик",
    "Профессор",
    "Курьер",
    "Художник",
    "Агент",
    "Шахматист",
)
