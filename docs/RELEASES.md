# GitHub, packages, APK and deployment

## Two repositories, different outputs

| Repository | Output | Destination |
| --- | --- | --- |
| accounts | Existing login/signup/manage web pages | Cloudflare Pages |
| accounts | OAuth/auth API Worker | Cloudflare Workers |
| accounts | Reviewed schema migrations | Existing Supabase project |
| accountsapp | core + ui AARs, POMs, sources | Maven-compatible distribution, initially JitPack |
| accountsapp | Signed standalone Gothwad Accounts APK | GitHub Releases |

Android needs no Cloudflare deployment. Both Android and web use the same Accounts service. AAR is for developers, APK is for users; uploading a raw AAR to Releases alone is not a Maven publication with transitive dependencies. GitHub Packages is optional, not required alongside JitPack. AAB is optional later for Play distribution, not a direct-install replacement for APK.

## Workflow rollout

`planning.yml` validates documentation and `android-ci.yml` now defines Android tests/lint/build and local Maven publication checks. Initial `jitpack.yml` is present. These have not been remotely run; signed release automation is still pending. Target rollout:

1. **android-ci.yml** — PR/push: pinned JDK/Gradle, formatting, JVM tests, lint, assemble libraries + standalone + both sample hosts; no signing secrets on PRs. Emulator tests for redirects/broker separately. Cache dependencies; do not cache credentials.
2. **release.yml** — explicit version tag/manual release gate on reviewed source: rerun validation, verify versionName/versionCode/tag alignment, build signed release APK, verify signature and expected public certificate, checksum assets, create draft GitHub Release. Upload libraries/sources as optional diagnostic assets. Missing signing secrets must fail, never generate replacement keys.
3. **JitPack configuration** — Gradle `maven-publish` for `core` and `ui`, generated POM dependencies and source jars. Test a clean external consumer before documenting coordinates as usable. Proposed multi-module coordinates: `com.github.GothwadTech.accountsapp:core:<tag>` and `com.github.GothwadTech.accountsapp:ui:<tag>`. Do not assume the aggregate single-artifact coordinate works.

Use immutable version tags. GitHub tag/release does not itself guarantee successful JitPack publication; request and verify that build separately. Keep SDK version and standalone versionCode explicit and monotonic. Initially one release train is simplest. Releases should include APK, SHA256SUMS, release notes, signing certificate fingerprint and SDK integration instructions.

## Owner checklist on GitHub (no credentials in chat)

- Enable Actions, protect main with required CI/review, protect release tags, and create a protected `release` environment with approval.
- Grant this repo access to org signing secrets: `RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (matches PlusTube naming). Verify the intended new-app key; keep old apps on their existing keys.
- Grant `contents: write` only to the release job; CI uses read-only permissions. Keep secrets away from fork/untrusted PR execution. Pin third-party Actions to reviewed commit SHAs when installing release workflows.
- Maintain off-platform key backup and documented public SHA-256 fingerprint. Delete temporary keystores even on failed release jobs; never upload them as artifacts.
- Register standalone + sample + consumer OAuth clients and exact redirect URIs in Accounts. Decide final callback hosts before production.
- Publish `/.well-known/assetlinks.json` on each App Link host, with actual application IDs and release fingerprints. FOSS certificates require their own link association if HTTPS callbacks are used, even when broker is off.

No manual long-lived GitHub token is needed for normal same-repo Releases: use job-scoped `GITHUB_TOKEN`.

## Accounts deployment

Existing setup documents Pages + Workers Builds Git integration; verify the actual Cloudflare dashboard before changing anything. Do not add a second automatic deploy pipeline on top of the existing one. If replacing with Actions later, use protected environments and scoped Cloudflare credentials only in `accounts`, never in Android.

Apply backwards-compatible migrations first, deploy compatible Worker next, then enable mobile clients/features. Feature-gate broker until server support is ready. Web UI changes deploy from `accounts`, not `accountsapp`. Set production DEV_MODE=false and verify routes, cookie settings, redirects and recovery flows before launch.

## Release gates

Real-device browser login/signup/cancel/process death; rotated-refresh concurrency and outage behavior; no credentials in logs/backups; standalone/embedded UI parity; FOSS fallback; external Maven consumer build; APK signature/install/update verification. Broker adds malicious caller, replay, stale trust registry and sign-out delivery tests.

Nothing in this planning change creates releases, changes secrets, deploys services or verifies current cloud settings.
