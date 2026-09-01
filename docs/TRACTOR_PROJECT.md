# Tractor project notes

The game-specific implementation lives in `games/maze`.

- `MazeGameView` handles grid movement, swipe direction queue, center-of-tile turns, collisions, collection and exit logic.
- `MazeLevelConfig` defines 45 playable construction mazes.
- `MazeSkins` contains procedural tractor rendering and site themes.
- `MazeOnboardingIllustrationView` draws onboarding scenes with a builder and tractor.

Only tractor-maze gameplay code and tractor-specific resources are included.
