# Builder Maze Tractor

Android-проект чисто под игру про трактор в строительном maze-лабиринте.

## Что внутри

- Maze-аркада в стиле Pac-Man: трактор едет автоматически по сетке, свайп ставит следующее направление, поворот выполняется только в центре тайла.
- 45 уровней на 5 паттернах сложности.
- Материалы: камни, брёвна, кирпичи. Нужно собрать всё, после этого открыт выезд.
- Статичные препятствия: кран, яма, бочка. Столкновение завершает попытку.
- Onboarding из 3 экранов с procedural-иллюстрацией игрового процесса, строителем в каске и кнопкой Next/Start.
- Гараж со скинами трактора и темами строительной площадки.
- Локальные достижения, монеты, прогресс уровней и таблица результатов через Room.
- Вся игровая графика отрисовывается процедурно через Canvas/XML/vector, без старых ассетов из исходного примера.

## Основные файлы

```text
app/src/main/java/com/nickaleush/tractormaze/games/maze/MazeGameView.kt
app/src/main/java/com/nickaleush/tractormaze/games/maze/MazeLevelConfig.kt
app/src/main/java/com/nickaleush/tractormaze/games/maze/MazeSkins.kt
app/src/main/java/com/nickaleush/tractormaze/games/maze/MazeOnboardingIllustrationView.kt
app/src/main/java/com/nickaleush/tractormaze/feature/game/GameFragment.kt
app/src/main/java/com/nickaleush/tractormaze/data/repository/GameRepository.kt
```

## Архитектура

Проект сохранён в стиле исходного примера:

- single-module Android app;
- XML layouts + ViewBinding;
- Navigation Component;
- Fragment + ViewModel;
- Repository + DAO + Room;
- AppServiceLocator вместо DI-фреймворка;
- Custom Canvas View для игрового поля.

## Ресурсы

Старые ассеты из исходного примера удалены: персонажи, игровые объекты, старые фоны, shop-картинки, launcher webp, шрифт и старая музыка. В проекте остались только Tractor-ресурсы: XML/vector UI, procedural Canvas-отрисовка и новые маленькие synthesized WAV-звуки.

## Сборка

```bash
./gradlew assembleDebug
```

В этой среде сборка Gradle не запускается до конца из-за отсутствия исходящего доступа к `services.gradle.org` для загрузки Gradle Wrapper. В Android Studio/локальной среде с интернетом wrapper скачает Gradle и соберёт проект.
