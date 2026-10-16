# Gothwad Accounts — new-chat handoff / project status

**Read this first in a new chat.** Updated 2026-10-16 (Asia/Calcutta).
This document distinguishes implemented source from verified functionality. Do not assume an SDK, APK or backend feature works just because its source is present.

## 1. Quick status / next action

- Repository: `GothwadTech/accountsapp`.
- PR: https://github.com/GothwadTech/accountsapp/pull/1
- Implementation commit reviewed here: `61ce8ad595902775cda00dd2993d0558669f721a`.
- Current PR branch: `arena/4eb3caf9-accountsapp`, base `main`. PR was **OPEN**, not merged, at this handoff check. User intends to merge and continue in a new chat; recheck live state rather than assuming the merge happened.
- Planning and initial Android browser-auth source are present. **No successful Android build or device login has been verified. No APK/AAR release has been published.**
- Immediate task: **fix Android CI setup, then run compilation/tests/lint and fix the code until green.** Do not begin broker implementation or production release before establishing this baseline.
- This handoff update changes documentation only; it does not fix CI or merge/deploy anything.

### Latest observed CI (for the implementation commit)

| Check | Observation |
| --- | --- |
| Planning checks | PASS, runs `38852758669` and `38852744972` |
| Android CI | FAIL, runs `38852758581` and `38852744928` |
| Failed step inspected | `android-actions/setup-android@9fc6c4e9069bf8d3d10b2204b1fb8f6ef7065407` |
| Earlier steps | Checkout and Java setup passed |
| Later steps | Gradle setup, static resource checks, compilation/tests/lint/publication checks were **skipped** |

Run: https://github.com/GothwadTech/accountsapp/actions/runs/38852758581
Job: https://github.com/GothwadTech/accountsapp/actions/runs/38852758581/job/116817413941

`gh run view 38852758581 --log-failed` could not retrieve the logs in the previous sandbox: the redirected `results-receiver.actions.githubusercontent.com` request returned EOF. Job metadata confirmed the failed step, but **the root cause is not known**. Do not invent a cause or describe the failure as a Kotlin compilation error.

Annotations also warn that some pinned Actions use deprecated Node 20, setup-java v4 is deprecated, and ubuntu-latest is scheduled to migrate to Ubuntu 26 on October 19, 2026. These are observations, **not confirmed causes of the SDK setup failure**. Review current supported Actions and runner/SDK versions; pin reviewed SHAs.

## 2. What the user wants — locked decisions

1. App name **Gothwad Accounts**, standalone application ID **`com.gothwad.accounts`**.
2. It must be independently installable and usable, AND the full account experience must be embedded in every Gothwad Android app. Standalone installation is **optional**, never a dependency of PlusTube/Browser/Mail/etc.
3. Standalone launch and a host's “Manage your Gothwad Account” must use the **same shared screen implementation/navigation**, not two similar-looking UIs maintained separately.
4. **Login/sign-in/signup must reuse the exact existing Accounts WEB UI.** Do not recreate or redesign credential forms in Compose. Use Custom Tabs + OAuth authorization code + PKCE S256, not WebView. Browser chrome is platform-controlled and cannot be promised pixel-identical across devices.
5. Browser session cookies can provide SSO, depending on the browser/profile. Signup and recovery stay on the existing website.
6. One shared native account-management experience is the intended end state; the current shell is only an initial step. Existing `/me` remains available in Custom Tabs while management APIs/screens are developed. Inventory actual backend capabilities first.
7. Identity/auth only: Tube subscriptions, recap and other app data belong to each app's own backend. Do not add them to this SDK.
8. Multi-account storage from day one; switching active account is local to the host app. Each app has its own client-specific tokens. Never share a refresh token across apps.
9. Future distributed broker: discover known sibling apps, verify caller certificate before even metadata access, show user-approved Continue-as, obtain target-specific credentials through a reviewed server handoff/exchange protocol. No mandatory central Accounts app.
10. Trust is `(package, signing SHA-256, client_id)`, not a cross-app signature permission: existing apps have different keys. New apps may use common org release key; do not change existing live app keys.
11. FOSS/untrusted-signature builds use browser auth with broker disabled. Unknown protocol/cert/server capability falls back gracefully.
12. Native shared UI uses Material 3 tokens: no hex colors, gradients, glass, custom shadows. Full-screen account theme shared across hosts; small host widgets may inherit host theme. English/Hindi and accessibility required.
13. Server revocation is authoritative for global/device-wide logout; verified IPC is only best-effort notification. UI must distinguish local removal from real server/global sign-out.
14. User approved security recommendations, including single-use target-bound handoff codes, host-restricted token injection, explicit sign-out scopes and callback hardening. Server proof of first-party app identity still requires design/review, not just `first_party=true` or public `client_id` checks.

## 3. Repository boundaries

| Repository | Responsibility |
| --- | --- |
| `GothwadTech/accounts` | Existing web UI, Cloudflare Worker OAuth/auth API, Supabase schema |
| `GothwadTech/accountsapp` | Android core/ui libraries, optional standalone app, test hosts, Android CI/releases |
| `GothwadTech/mail` | Consumer reference: userinfo validation and 401 versus 503 handling |
| `GothwadTech/tubeapp` | First intended consumer, PlusTube; integrate in a separate session after SDK validation |
| `GothwadTech/keystore` | Signing-key tooling reference; README alone did not establish actual secrets or certificate fingerprints |

Android needs **no second Cloudflare backend deployment**. Web and Android use the same Accounts service.

Referenced Accounts main observed at `a00407a5ac48376fce237bf96696d133d4792f74`; recheck for changes. Relevant references reviewed: Accounts `docs/ARCHITECTURE.md`, `docs/OAUTH.md`, Worker routes, core SQL; Mail session/auth redirect sources; PlusTube AGENTS/build/signing references. A complete audit of all referenced consumer features was not done.

Backend observations (source, not live deployment verification):
- Browser API base `https://accounts.gothwadtech.com/api`; Worker-to-Worker custom domain `https://api.gothwadtech.com`.
- Public clients require PKCE; access tokens last one hour, rotating refresh tokens 30 days with offline_access. Identity comes from `/oauth/userinfo`, not local JWT parsing.
- Authenticated `/oauth/authorize` currently renders consent; first-party consent skipping is not implemented in this change.
- `gothwad-tube` is seeded in SQL but its seed redirect list is empty; deployed DB values are unknown.
- Existing `/oauth/revoke` is browser-session-based, not a proven native bearer-token revocation API.
- Checked-in Wrangler config contains `DEV_MODE = "true"`. **Audit production and ensure false before release.** We did not verify the live value.
- Accounts docs describe Cloudflare Pages + Workers Builds Git deployment. Dashboard wiring is unknown; do not introduce duplicate deployment pipelines blindly.

## 4. What is implemented in source

### Build structure

- `core/`: namespace `com.gothwad.accounts.core`, no Compose.
- `ui/`: namespace `com.gothwad.accounts.ui`, shared Compose M3 account shell.
- `app/`: thin standalone host, `com.gothwad.accounts`.
- `sample/`: alpha/beta flavors with distinct application IDs, to install side by side.
- Gradle wrapper 8.13, AGP 8.13.2, Kotlin 2.2.21, JDK 17, compile/target SDK 36, minSdk 26.
- This is a conservative pinned baseline, **not verified latest stable**. Original request preferred latest stable; reconcile after establishing a reproducible build.
- `maven-publish` release AAR/source publications and initial `jitpack.yml` exist, but no external Maven consumer has successfully resolved them yet.

### Core source

- `GothwadConfig`, `Scope`, `BrokerPolicy.Disabled`.
- `GothwadAccounts.init`, state/error flows, signIn, switchTo, local signOut, manageOnWeb and network auth interceptor.
- PKCE uses SecureRandom, SHA-256 and unpadded base64url platform primitives.
- Callback checks exact redirect scheme/authority/path, state, duplicates, expiry, code/error exclusivity; pending login stored encrypted and consumed once.
- Tink/Android Keystore encrypted atomic session file under noBackupFilesDir; client/environment/redirect-bound storage.
- User identity fetched from userinfo; no JWT identity parsing.
- Per-account refresh serialization, rotated token persistence, transient outage retention, protection against refresh resurrecting removed sessions.
- `AccountCallbackActivity` returns to host launcher. Cold process/browser/task behavior needs device testing.
- Async UI actions return Jobs and report sanitized failures through error StateFlow. Request-time refresh errors propagate to HTTP callers.

### UI source

- `GothwadAccountsTheme`, `GothwadAccountsScreen`, `SignInButton`.
- AccountManagementActivity and `GothwadAccounts.openAccountManagement(activity)` extension in the **ui** package.
- Standalone and both samples render the same account shell.
- Basic account list/switch/add/local removal; hosted advanced management via `/me`.
- English/Hindi string resources.
- No local login/signup credential forms.

### Important API differences from the original target

- Install auth using **`addNetworkInterceptor`**, NOT `addInterceptor`; runtime guard rejects application-interceptor installation. Every redirect hop must be host-checked.
- Interceptor owns/removes Authorization on disallowed requests; use a dedicated HTTP client, HTTPS port 443, explicit host allowlist.
- No account-partitioned HTTP cache: keep authenticated client uncached or partition/clear by account.
- `signOut(userId)` currently means **local removal only**; UI explicitly says this. No server revoke/global sign-out claim.
- States currently Initializing / SignedOut / SignedIn. Separate refreshing status/complete retry UX is pending.
- Full native management, avatar/sheets/settings drop-ins, and browser/native active-account alignment are not finished. A native active account and browser `/me` account can differ.

## 5. Tests and verification — do not overclaim

Implemented 12 JVM test cases in `core/src/test`:
- PKCE RFC vector and verifier format/uniqueness.
- Callback/config binding, invalid/duplicate parameters and denial.
- Concurrent successful refresh rotates once.
- Offline outage retains account; invalid grant removes it.
- Logout while refresh in flight does not resurrect account.
- Pending-login restore and single-use consumption.
- Failed persistence does not publish local removal.

**These tests have not executed successfully yet.** They are test sources, not a passing test report.

Locally passed:
- `python3 tools/check_docs.py`
- `python3 tools/check_android_scaffold.py` (XML parsing, locale keys, TOML, wrapper hash, module structure)
- `git diff --check` / staged whitespace checks

Local `./gradlew --version` failed because Java is absent. Previous sandbox also lacked Android SDK and outbound access to all required Android dependency hosts. GitHub CI later failed in SDK setup, before compilation. Real-device/emulator tests, actual login, encrypted storage behavior, release signatures and Maven resolution are all pending.

## 6. Remaining roadmap / acceptance gates

| Stage | Status | Remaining |
| --- | --- | --- |
| Plan + module boundaries + API | Initial docs complete | Keep API/docs synchronized as code is validated |
| Browser-auth core + sample | Source baseline only, not validated | CI fix; compilation/tests/lint; backend registration; device flows, outages and process death |
| Shared account UI | Initial shell only | Full parity/navigation, avatar/chooser/sheets/settings, management APIs, accessibility/device tests |
| Accounts backend changes | Plan only | Separate reviewed PR, migrations, endpoints, deploy coordination |
| Broker + trusted apps | Not implemented | Secure server proof, signed registry, caller validation, IPC, replay protection, two-host tests |
| Release/distribution | CI/publishing config only | Signed release workflow, signing secrets, APK verification, external Maven consumer test |
| PlusTube integration | Not started | Separate tubeapp session after SDK milestone |

### Recommended next execution order

1. Re-read this file, current code/docs and live PR/Actions status; respect the new session's assigned branch.
2. Retrieve SDK setup failure logs via GitHub UI/CLI in a permitted environment. Fix setup; review pinned Actions and SDK installation. Do not weaken checks to get green.
3. Run unit tests/lint/assemble all hosts and library Maven publication; fix compilation/runtime design issues found. Add missing tests especially HTTP errors/redirect credential leakage, expired callbacks and storage/rotation failures.
4. Provide debug APK artifacts through CI for device testing (currently no artifact-upload step). Do not call them signed production releases.
5. Coordinate public OAuth client registration and callback routing with Accounts; verify source plus live configuration.
6. Exercise existing web auth and real callbacks on device (including process death and two accounts), not just a successful build.
7. Complete shared UI and account management without recreating hosted credential screens or inventing unsupported APIs.
8. Implement reviewed Accounts backend changes in its own repository/session/PR. Only then enable broker.
9. Finish release signing/publication and perform clean consumer/install/update tests before tagging a public release.

### OAuth development registrations needed

These are proposed IDs configured in source, NOT confirmed registrations:

| Host | client_id | redirect_uri |
| --- | --- | --- |
| Standalone | gothwad-accounts-android | com.gothwad.accounts:/oauth/callback |
| Sample alpha | gothwad-accounts-sample-alpha | com.gothwad.accounts.sample.alpha:/oauth/callback |
| Sample beta | gothwad-accounts-sample-beta | com.gothwad.accounts.sample.beta:/oauth/callback |

Public clients, scopes profile/email/offline_access, no embedded client secret. Source hosts use fallback schemes for development. Production should prefer verified HTTPS App Links with real assetlinks.json and actual release certificate fingerprints. Final production callback hosts still need agreement/registration.

## 7. Server / broker constraints still unresolved

See [SERVER_CHANGES.md](SERVER_CHANGES.md), [BROKER_PROTOCOL.md](BROKER_PROTOCOL.md) and [SECURITY.md](SECURITY.md).

- Token exchange must not trust public client_id or first_party flags as proof of caller app identity.
- Proposed one-time handoff code is target/client/challenge/user/scope bound. PKCE-like binding prevents intercepted-code redemption but does not itself authenticate an app installation to the Worker.
- First-party consent skip must preserve account selection, login, PKCE, redirect and scope checks.
- Signed trusted-app registry needs pinned verification keys, expiry/version/rollback protection and key rotation.
- Verify Binder UID/package/certificate BEFORE metadata disclosure. API 28+ SigningInfo/hasSigningCertificate; API 26–27 legacy GET_SIGNATURES (not GET_SIGNING_CERTIFICATES).
- No shared refresh tokens. No credentials or debug keys added to production trust lists.
- Global/device sign-out needs native revoke API and server session-family/device semantics. Broadcast extras do not prove sender identity.
- Existing Worker source uses multi-step code burn / refresh rotation; review atomicity and failure recovery before treating it as race-safe.
- Client storage/rotation failure recovery, multi-process behavior and unrecoverable Keystore UX need review. Current SessionManager coordination is in-process; do not introduce separate processes accessing its store without additional design.

## 8. Release/deployment and owner actions

- `accounts`: existing frontend → Pages, Worker → Workers, reviewed migrations → existing Supabase.
- `accountsapp`: core/ui AAR + POM + source jars → Maven distribution (initially JitPack); standalone signed APK → GitHub Releases. AAB only if later needed for Play.
- Proposed multi-module coordinates: `com.github.GothwadTech.accountsapp:core:<tag>` and `...:ui:<tag>`. **Unverified/unpublished.**
- No signed release workflow yet. No release tags, APK/AAR publication, cloud deployments or database changes were performed.
- Owner will need repo access to org signing secrets: RELEASE_KEYSTORE_BASE64, STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD. Never ask for secrets in chat or commit them. Confirm public cert fingerprints and backups.
- Configure appropriate branch checks/release environment approval; production only after green CI and device verification. Do not auto-merge or deploy without authorization.

## 9. Documentation map

- [ARCHITECTURE.md](ARCHITECTURE.md): system boundaries and milestones.
- [PUBLIC_API.md](PUBLIC_API.md): implemented API and caveats (use this over the old aspirational prompt).
- [INTEGRATION.md](INTEGRATION.md): initialization, callback manifest, commands and device checklist.
- [RELEASES.md](RELEASES.md): GitHub/release/deployment plan.
- [SERVER_CHANGES.md](SERVER_CHANGES.md): separate Accounts PR plan.
- [SECURITY.md](SECURITY.md): security requirements.
- [BROKER_PROTOCOL.md](BROKER_PROTOCOL.md): disabled draft, not shipped functionality.

## 10. Copy/paste prompt for the next chat

> Work on GothwadTech/accountsapp. First read docs/HANDOFF.md, README.md, docs/PUBLIC_API.md and docs/INTEGRATION.md, then inspect the live repository and Actions status. Preserve all locked user requirements: optional standalone Gothwad Accounts (com.gothwad.accounts), same embedded account screens, and existing Accounts WEB login/signup via Custom Tabs, never recreated native credential forms. PR #1 contains the initial source baseline, not a verified release. Last observed Android CI failed during Android SDK setup before compilation; diagnose and fix CI first, then compile/test/lint all modules and fix failures. Do not assume the PR has merged or that backend clients exist. Continue browser-auth validation and device testing before broker/release work. Read the rest of the roadmap in HANDOFF.md and update it as work progresses. Never claim tests passed unless they actually ran; do not deploy/merge without authorization.
