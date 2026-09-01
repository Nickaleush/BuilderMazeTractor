# Builder Maze Tractor — Release Guide

## Debug build

```bash
./gradlew assembleDebug
```

## Release build

Создай `keystore.properties` в корне проекта по шаблону `keystore.properties.template`, затем выполни:

```bash
./gradlew bundleRelease
```

## Signing template

```properties
storeFile=/absolute/path/to/tractor-release.keystore
storePassword=********
keyAlias=tractor
keyPassword=********
```

## Перед публикацией

- Проверь `applicationId` в `app/build.gradle.kts`.
- Прогони уровни 1–5 и несколько уровней из 6–45.
- Проверь onboarding, гараж, настройки звука/музыки, достижения и сохранение прогресса.
- При необходимости замени procedural/vector launcher icon на финальную иконку из Image Asset Studio.
