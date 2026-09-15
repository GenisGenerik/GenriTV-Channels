# GenriTV

GenriTV is an Android TV-first streaming application for live TV, movies, and series. The project is being evolved toward a production-grade architecture inspired by professional streaming platforms.

## Architecture

The application follows a layered approach:

- `model/` — domain-facing data models.
- `data/` — repositories, parsers, source adapters, and caching.
- `ui/` — Compose TV screens, navigation, and presentation state.
- `ui/theme/` — shared visual system.

Keep business rules independent from UI concerns. Prefer small repositories/services over putting data and playback logic into activities or composables.

## Product direction

The roadmap follows the executive brief:

1. Reliable adaptive HLS/DASH playback and robust fallback handling.
2. Netflix/Vidio-inspired dark, content-first TV UX with strong focus states.
3. Search, categories, metadata, watch history, and personalization foundations.
4. Accessibility, keyboard/remote navigation, subtitles, and localization.
5. Automated unit/integration testing and CI quality gates.
6. Security, observability, release automation, and production documentation.
7. Backend/media infrastructure can be introduced separately when premium licensed content requires authentication, tokenized URLs, CDN, DRM, transcoding, or scalable APIs.

## Development

Open the project in Android Studio with JDK 17. Run:

```bash
./gradlew testDebugUnitTest
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
```

## Streaming and rights

Only use streams and media for which you have permission to distribute. DRM, tokenized URLs, CDN delivery, transcoding, authentication, and backend services belong to a future production media stack and are not simulated as complete in the Android client.

## Quality gates

Every change should pass linting, unit tests, and debug/release compilation in GitHub Actions. Critical playback, search, navigation, and accessibility behavior should gain regression tests before release.
