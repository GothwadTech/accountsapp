# Architecture — agreed direction

Planning baseline: 2026-10-16. Browser-auth source baseline now exists; Android compilation and device verification remain pending.

## Responsibilities

`GothwadTech/accounts` owns the existing web UI, OAuth provider, Worker and Supabase schema. `GothwadTech/accountsapp` owns Android libraries and the optional Gothwad Accounts app (`com.gothwad.accounts`). No second identity backend or Android-specific database deployment.

```
Standalone app ─┐
PlusTube ───────┼─ shared ui → core → Accounts OAuth / Worker
Other apps ────┘                └─ Custom Tabs → existing web auth
```

Login, signup, recovery and consent use the existing web implementation, not a Compose recreation. Browser chrome is platform-controlled. Browser-cookie SSO depends on the chosen browser/profile. Native encrypted sessions and browser cookies are distinct; never copy cookies or put tokens into URLs.

Standalone launch and embedded Manage account open the same shared management root. Full-screen UI shares a theme; small host controls may inherit the host theme. M3 tokens only, no hardcoded hex colors, gradients, glass or custom shadows. English/Hindi resources and accessibility required. Existing `/me` capabilities must be inventoried before defining native management features: OAuth identity scopes do not automatically authorize profile/security mutations. Unsupported sensitive operations use browser reauthentication until explicit APIs exist.

## Modules

- `core`: namespace `com.gothwad.accounts.core`; config, PKCE/Custom Tabs, callback activity, encrypted per-user TokenStore, StateFlow session manager, userinfo, restricted OkHttp interceptor; broker later.
- `ui`: namespace `com.gothwad.accounts.ui`; shared management navigation and screens, chooser, avatar, account sheet, sign-in button, settings section.
- `app`: application ID `com.gothwad.accounts`; thin standalone host, own registered OAuth client.
- `sample`: two application IDs and OAuth clients for concurrent device testing. Production trust never includes debug keys.

All consumers have isolated credentials. Switching active account is local to the host. Core has no Compose dependency. SDK upgrades require host app updates; hosted auth page updates do not.

## Milestones / acceptance gates

1. Planning skeleton and API (completed); current implementation status is in README.
2. Pin verified stable AGP/Kotlin/Gradle versions, wrapper and dependencies; browser PKCE auth, process-death-safe callback, encrypted sessions, refresh and sample. Validate real registered redirects on device.
3. Shared management and account UI; verify standalone/embedded parity, locale, accessibility, dark mode.
4. Accounts backend contract/migrations and separate PR; maintain old web/consumer compatibility.
5. Broker enabled only after server authentication/replay/revocation design passes review; two-app device tests, unknown-cert/version/FOSS fallback.
6. Signed standalone APK, independently consumable Maven modules, integration guide and release smoke tests. PlusTube integration is a separate session.

## Repository observations

Reviewed Accounts architecture/OAuth docs, Worker routes and core SQL (Accounts main observed at `a00407a5ac48376fce237bf96696d133d4792f74`), Mail session/auth redirect references, PlusTube AGENTS/build/signing references and keystore README.

- Accounts has authorization-code and rotating refresh grants; `/oauth/authorize` currently renders consent for authenticated users.
- SQL seeds `gothwad-tube` with empty redirect list; actual deployed DB registration is not verified.
- Existing `/oauth/revoke` is browser-session based, not proof of a native-token revocation API.
- Mail distinguishes upstream auth denial from availability failure; retain this rule in Android.
- Accounts docs specify Pages frontend and Workers Builds deployment. No `.github/workflows` directory was found there; dashboard wiring is not verified.
- Checked-in Wrangler config has `DEV_MODE = "true"`. Production config must be audited and false before release; this is not evidence of the live setting.
- Keystore README does not establish signing key ownership or secret availability. Verify public certificate fingerprints; never retrieve private keys for planning.
