# Changelog

Все заметные изменения проекта. Формат близок к [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
версии — по [SemVer](https://semver.org/lang/ru/).

Релизы выпускаются пушем git-тега `vX.Y.Z`: workflow `android-release.yml` собирает
подписанный APK и публикует его в [GitHub Release](https://github.com/malyi-m-dev/filmax/releases)
с авто-changelog из коммитов (Conventional Commits) между тегами. Этот файл —
кураторская выжимка поверх авто-генерации.

## [Unreleased]

## [1.6.0] — 2026-07-21

### Добавлено
- Телеметрия ошибок (Firebase Crashlytics): non-fatal ошибки сети/парсинга с URL и кодом,
  ошибки воспроизведения, user id (username), версия приложения; debug-сборки не отправляют.
- Мобильный плеер догнал TV: кнопка «Следующая серия», автопереход в конце серии,
  спиннер буферизации, рабочая кнопка «Повторить» в модалке ошибки.
- Подпись версии приложения внизу Профиля (mobile и TV).

### Исправлено
- Плеер перебирает варианты доставки потока (hls4 → hls → http), когда CDN недоступен
  (DPI/SNI-блокировки api.srvkp.com) — лечит «следующая серия не запускалась».
- TV-плеер показывает карточку ошибки воспроизведения вместо немого чёрного экрана
  и спиннер во время буферизации.
- Выход из плеера кнопкой HOME больше не оставляет играющий звук: уход в фон ставит паузу.
- «Назад» с онбординга закрывает приложение; онбординг больше не встаёт поверх
  авторизованных экранов при протухании сессии.

## [1.0.0] — 2026-07-07

Первый публичный релиз. Приложение раздаётся как APK (Google Play не используется).

### Добавлено
- Релизный флоу: подпись release (`signingConfig` из `keystore.properties`/CI-секретов),
  R8 + `shrinkResources`, `versionCode` из числа коммитов и `versionName` из git-тега.
- CI по тегу `v*`: сборка подписанного APK → GitHub Release (+ Telegram).

[Unreleased]: https://github.com/malyi-m-dev/filmax/compare/v1.6.0...HEAD
[1.6.0]: https://github.com/malyi-m-dev/filmax/releases/tag/v1.6.0
[1.0.0]: https://github.com/malyi-m-dev/filmax/releases/tag/v1.0.0
