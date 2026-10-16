# Gothwad Accounts

Optional standalone Android app **Gothwad Accounts** (`com.gothwad.accounts`) and embedded account SDK. Minimum Android API: 26.

**Status: early browser-auth implementation, not production-ready.** Android build/device verification and live OAuth client registration are still required. No APK or SDK version has been published.

**New chat / next maintainer: read [docs/HANDOFF.md](docs/HANDOFF.md) first.** It records decisions, completed work, pending work, and the observed Android CI setup failure.

## Implemented in source

- Gradle modules: `core` (no Compose), `ui`, standalone `app`, and two installable `sample` flavors.
- Existing Accounts web login/signup in Custom Tabs; PKCE S256, bound single-use callbacks and encrypted pending-login persistence.
- Tink / Android Keystore encrypted no-backup token storage, userinfo identity, multi-account state, per-account refresh serialization and local removal.
- HTTPS/host-restricted **network** bearer interceptor, checked on every network redirect hop.
- Shared M3 account shell with English/Hindi resources; standalone and embedded management use the same screen. Full management remains on the existing `/me` website for now.
- JVM test sources, Android CI and initial Maven/JitPack configuration.

Broker, trusted certificate registry, global sign-out, native server revocation, complete management UI and signed release automation are **not implemented yet**. Standalone installation is never required by consumers.

## Build

Requires JDK 17, Android SDK 36 and access to Google Maven/Maven Central/Gradle distribution servers.

```sh
./gradlew :core:testDebugUnitTest :app:assembleDebug \
  :sample:assembleAlphaDebug :sample:assembleBetaDebug
```

Register the clients and callbacks in [integration](docs/INTEGRATION.md) before attempting real login. Debug APKs are not production releases.

## Documentation

- [Architecture and milestones](docs/ARCHITECTURE.md)
- [Current public API and limitations](docs/PUBLIC_API.md)
- [Integration / device checklist](docs/INTEGRATION.md)
- [GitHub, publishing and deployment](docs/RELEASES.md)
- [Accounts backend changes](docs/SERVER_CHANGES.md)
- [Security requirements](docs/SECURITY.md)
- [Broker protocol draft](docs/BROKER_PROTOCOL.md)

Local static checks: `python3 tools/check_docs.py && python3 tools/check_android_scaffold.py`.
