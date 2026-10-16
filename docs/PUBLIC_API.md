# Current development API

Initialize once from `Application.onCreate`, including after process recreation:

```kotlin
GothwadAccounts.init(this, GothwadConfig(
    clientId = "gothwad-tube",
    redirectUri = "https://tube.gothwadtech.com/oauth/callback",
))
```

```kotlin
val state by GothwadAccounts.state.collectAsStateWithLifecycle()
val error by GothwadAccounts.error.collectAsStateWithLifecycle()
GothwadAccounts.signIn(activity) // Existing web authentication
GothwadAccounts.signIn(activity, chooseAccount = true) // Existing web account chooser
GothwadAccounts.switchTo(userId)
GothwadAccounts.signOut(userId) // Local removal ONLY in this development version
```

Mutations return coroutine Jobs; failures are exposed through `error` (sanitized `AuthFailure`), not thrown to the UI. `clearError()` dismisses the message. Browser toolbar close may produce no OAuth callback: the UI remains usable and a new login replaces the old pending transaction. Transactions expire after ten minutes.

State is `Initializing`, `SignedOut`, or `SignedIn(account, all)`. Refresh keeps the current account visible. Request-time refresh failures propagate to the HTTP caller; transient errors do not erase credentials. Separate refreshing/retry UX remains follow-up work.

From the `ui` module:

```kotlin
import com.gothwad.accounts.ui.openAccountManagement
GothwadAccounts.openAccountManagement(activity)
// Or embed the same root in your Compose navigation:
GothwadAccountsTheme { GothwadAccountsScreen() }
```

Current components are `GothwadAccountsScreen`, `GothwadAccountsTheme`, and `SignInButton`. Avatar/sheets/settings drop-ins and full native management navigation are not implemented yet. `manageOnWeb(activity)` opens existing `/me` in Custom Tabs; browser account and active native account can differ. No token is put in the URL to bridge them.

## Network authorization

```kotlin
val client = OkHttpClient.Builder()
    .addNetworkInterceptor(GothwadAccounts.authInterceptor(
        allowedHosts = setOf("tube-api.gothwadtech.com"),
    ))
    .build()
```

Hostnames above are examples, not deployment confirmations. **Use addNetworkInterceptor, not addInterceptor**: each redirect hop is checked. This interceptor owns the Authorization header and removes it on disallowed requests; use a dedicated client for Gothwad-authenticated traffic. HTTPS default port 443 only. Cached responses are not account-partitioned by this library: do not share an authenticated response cache across users. Use OkHttp's default no-cache configuration or explicitly partition/clear it on account switch and logout. Never invoke blocking HTTP calls on the main thread.

Tokens refresh proactively within 60 seconds of expiry. There is no automatic resource-server 401 retry yet. The backend must independently authorize token audience and scopes; a host allowlist does not replace server authorization.

Config is immutable for one app process. BrokerPolicy currently exposes only Disabled. All clients in this baseline must use the browser-facing Accounts host for authBaseUrl (default `https://accounts.gothwadtech.com/api`); hosted `/signin` and `/me` are derived from that origin.
