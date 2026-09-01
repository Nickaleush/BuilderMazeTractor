# Builder Maze Tractor implementation

This project keeps the original starter architecture: single Android app module, XML + ViewBinding screens, Navigation component, Room repositories, service locator, custom Canvas-based game view, shop, achievements, levels, result screen, settings and audio.

## Main gameplay files

- `games/maze/MazeGameView.kt` — Pac-Man-like tile movement: the tractor moves automatically; swipe queues direction; turn happens only on tile centers; blocked tile / obstacle causes crash; all materials must be collected before the exit opens.
- `games/maze/MazeLevelConfig.kt` — 45 levels using 5 reusable maze patterns with speed ramp. Materials and static obstacles are encoded directly in the tile maps.
- `games/maze/MazeSkins.kt` — procedural tractor skins, site themes, shop preview renderer.
- `games/maze/MazeOnboardingIllustrationView.kt` — tutorial pictures: gameplay in a frame plus builder character in a hard hat.
- `games/maze/MazeGameResult.kt` — result model saved by the repository.

## Feature screens touched

- `feature/splash/SplashFragment.kt` — routes first launch to onboarding, next launches to menu.
- `feature/onboarding/OnboardingFragment.kt` + `res/layout/fragment_onboarding.xml` — 3-page onboarding with Next/Start button.
- `feature/game/GameFragment.kt` — connects MazeGameView with audio, HUD and result saving.
- `feature/levels/LevelsFragment.kt` — level cells describe number of materials.
- `feature/shop/ShopFragment.kt` — uses MazePreviewView for tractor/theme cards.
- `data/repository/GameRepository.kt` — construction skins/themes, achievements, level unlocks, coins.

## Notes

Gradle wrapper build could not be verified in this sandbox because `gradlew` attempted to download Gradle from `services.gradle.org`, and outbound network is unavailable. The project files and XML were validated locally for path/resource consistency where possible.
