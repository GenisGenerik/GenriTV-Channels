# GenriTV Production Architecture

This document turns the executive brief into concrete boundaries for the current Android TV client and the future production media platform.

## Current client boundaries

```text
UI (Compose for TV)
        |
        v
Presentation state / ViewModels
        |
        v
Repositories + domain rules
        |
        +--> channel source / cache
        +--> VOD source
        +--> series source
        |
        v
Media3 playback boundary
        |
        +--> adaptive HLS/DASH where the source supports it
        +--> stream URL validation
        +--> source fallback
        +--> playback telemetry
```

The Android client must not contain server credentials, DRM secrets, or privileged media URLs.

## Production media stack boundary

```text
Client
  |
  +--> API / Auth service
  |       |
  |       +--> PostgreSQL (accounts, metadata, entitlements)
  |       +--> Redis (cache/session/rate-limit)
  |
  +--> CDN
          |
          +--> Object storage
          |
          +--> HLS/DASH/CMAF manifests
          |
          +--> DRM license service

Ingest -> Transcode/Package -> Storage -> CDN -> Client
```

The executive brief recommends a separately deployable API/auth/media infrastructure so business rules do not depend on infrastructure details. The current repository intentionally implements only the Android client side of that boundary.

## Playback contract

1. Validate all stream URLs before creating a `MediaItem`.
2. Prefer HTTPS and reject malformed/local URLs.
3. Allow adaptive bitrate when the manifest advertises multiple variants.
4. Retry a limited number of alternative stream sources automatically.
5. Expose buffering/ready/error state to the UI.
6. Record privacy-safe playback metrics: startup time, buffering events/time, selected resolution, bitrate, and error code.
7. Do not log full stream URLs or user-identifying data.

## Security boundary

Premium streams should eventually be issued by the backend using short-lived authorization and tokenized delivery. DRM license exchange must remain server-controlled. Never put signing keys, API secrets, or license credentials in the APK.

## Observability boundary

The client currently emits privacy-safe playback telemetry through Android logging as a local diagnostic foundation. A production build should forward equivalent structured events to the chosen observability platform without logging credentials or raw media URLs.

Recommended production metrics include playback startup time, rebuffer ratio, playback error rate, stream source fallback count, and client crash rate.

## Testing strategy

The repository uses a testing pyramid:

- JVM unit tests for data rules and validation.
- Android instrumentation smoke tests for the critical TV home journey.
- Additional playback/search/navigation E2E scenarios should be added as the product surface grows.

The CI pipeline gates changes with unit tests, lint, an Android TV smoke test, debug build, and release build.

## Not yet implemented

These are integration-level capabilities and require infrastructure/provider decisions before they can be honestly marked complete:

- user authentication and OAuth2/OIDC
- entitlement service and tokenized media URLs
- Widevine/FairPlay/PlayReady DRM licensing
- dedicated transcoding/packaging cluster
- CDN distribution
- PostgreSQL/Redis backend
- centralized metrics, dashboards, and alerting
- recommendation/personalization service

They are kept as explicit boundaries rather than fake client-side implementations.
