# Development integration and verification

This checkout is an unreleased development SDK. Use project dependencies while validating; Maven coordinates must not be advertised as published until an external resolution test succeeds.

## Modules

```kotlin
implementation(project(":ui")) // Includes core; standalone/shared management UI
// Or implementation(project(":core")) for a headless host.
```

Initialize in Application, collect state, and launch signIn as described in [public API](PUBLIC_API.md). Add a host callback intent filter: the library declares the exported callback Activity, but intentionally does not claim every host's callback URL.

```xml
<activity android:name="com.gothwad.accounts.core.AccountCallbackActivity"
    android:exported="true">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https" android:host="tube.gothwadtech.com"
            android:path="/oauth/callback" />
    </intent-filter>
</activity>
```

Example host only: publish a real assetlinks.json with the actual package and release certificate before claiming verified App Links. Callback validation checks exact scheme/authority/path, state, duplicates, code/error exclusivity and expiry. App Links need actual device verification, not only a manifest declaration.

## Development clients requiring backend registration

These IDs are proposed by this checkout, NOT verified live registrations:

| Host | client_id | redirect_uri |
| --- | --- | --- |
| Standalone | gothwad-accounts-android | com.gothwad.accounts:/oauth/callback |
| Sample alpha | gothwad-accounts-sample-alpha | com.gothwad.accounts.sample.alpha:/oauth/callback |
| Sample beta | gothwad-accounts-sample-beta | com.gothwad.accounts.sample.beta:/oauth/callback |

Register as public clients, allow `profile email offline_access`, and use exact redirects. No client secret in APK. The checked-in hosts use custom schemes for development; production should prefer verified HTTPS links. Do not add debug certificates to the production broker registry. Two sample packages test independent login/storage today, not broker handoff.

## Commands

```sh
./gradlew :core:testDebugUnitTest :core:lintDebug :ui:lintDebug
./gradlew :app:assembleDebug :sample:assembleAlphaDebug :sample:assembleBetaDebug
./gradlew :core:publishReleasePublicationToMavenLocal :ui:publishReleasePublicationToMavenLocal
```

Source dependencies are pinned to a conservative AGP 8.13.2 / Kotlin 2.2.21 / Gradle 8.13 baseline, not asserted to be the latest stable release. Reconcile with the latest verified stable toolchain before release; do not copy PlusTube alpha Compose dependencies blindly. The sandbox has neither Java nor Android SDK and cannot access all Android dependency hosts, so Gradle compilation/test execution is unverified here. GitHub Android CI is configured but has not been pushed or run in this session.

## Device acceptance (pending)

- Register clients; install standalone and both sample variants.
- Existing browser session, fresh signup, chooser, denial, toolbar close, missing browser.
- Kill host process while browser is open; callback restores through Application and encrypted pending request. Replayed and wrong-state callbacks must not sign in.
- HTTPS association verification, custom scheme interception resilience via PKCE.
- Two accounts, active switching, process restart, offline/503 persistence, expiry and rotated refresh.
- Slow refresh + local removal; no session resurrection. Keystore loss/corruption fails closed, no plaintext fallback. Recovery UX for unrecoverable storage remains a follow-up.
- Same shared UI in standalone and embedded Activity; Hindi/dark mode/accessibility, browser chrome distinguished from hosted page.
- Credential isolation and host/redirect Authorization leakage checks. No production release until these are exercised.

Local removal is deliberately labeled as such. It does not revoke browser cookies or refresh tokens on the server. Native revocation and device-wide logout are blocked on Accounts backend work.
