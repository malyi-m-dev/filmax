# Changelog

Все заметные изменения проекта. Формат близок к [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
версии — по [SemVer](https://semver.org/lang/ru/).

Релизы выпускаются пушем git-тега `vX.Y.Z`: workflow `android-release.yml` собирает
подписанный APK и публикует его в [GitHub Release](https://github.com/malyi-m-dev/filmax/releases)
с авто-changelog из коммитов (Conventional Commits) между тегами. Этот файл —
кураторская выжимка поверх авто-генерации.

## [Unreleased]

## [1.0.0] — 2026-07-07

Первый публичный релиз. Приложение раздаётся как APK (Google Play не используется).

### Добавлено
- Релизный флоу: подпись release (`signingConfig` из `keystore.properties`/CI-секретов),
  R8 + `shrinkResources`, `versionCode` из числа коммитов и `versionName` из git-тега.
- CI по тегу `v*`: сборка подписанного APK → GitHub Release (+ Telegram).

[Unreleased]: https://github.com/malyi-m-dev/filmax/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/malyi-m-dev/filmax/releases/tag/v1.0.0
