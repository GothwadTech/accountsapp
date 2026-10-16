package com.gothwad.accounts.core

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Looper
import androidx.browser.customtabs.CustomTabsIntent
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor

object GothwadAccounts {
    private lateinit var config: GothwadConfig
    private lateinit var session: SessionManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ready: kotlinx.coroutines.Deferred<Unit>? = null
    private val mutableError = MutableStateFlow<AuthFailure?>(null)
    val error: StateFlow<AuthFailure?> = mutableError.asStateFlow()
    val state: StateFlow<AccountState> get() = session.state

    @Synchronized
    fun init(application: Application, config: GothwadConfig) {
        if (ready != null) { require(this.config == config); return }
        this.config = config
        session = SessionManager(EncryptedTokenStore(application, config), OAuthClient(config))
        ready = scope.async {
            try { session.load() } catch (error: Exception) {
                mutableError.value = AuthFailure.StorageUnavailable
                throw error
            }
        }
    }

    fun manageOnWeb(activity: Activity) = scope.launch {
        action {
            readyOrThrow()
            val base = Uri.parse(config.authBaseUrl)
            val uri = base.buildUpon().path("/me").clearQuery().build()
            withContext(Dispatchers.Main) { CustomTabsIntent.Builder().build().launchUrl(activity, uri) }
        }
    }

    fun clearError() { mutableError.value = null }

    fun signIn(activity: Activity, chooseAccount: Boolean = false) = scope.launch {
        action {
            readyOrThrow()
            val pending = session.beginLogin()
            val uri = Uri.parse("${config.authBaseUrl.trimEnd('/')}/oauth/authorize").buildUpon()
                .appendQueryParameter("client_id", config.clientId)
                .appendQueryParameter("redirect_uri", config.redirectUri)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", config.scopes.map { it.wireValue }.sorted().joinToString(" "))
                .appendQueryParameter("state", pending.state)
                .appendQueryParameter("code_challenge", Pkce.challenge(pending.verifier))
                .appendQueryParameter("code_challenge_method", "S256").build()
            val destination = if (chooseAccount) {
                Uri.parse(config.authBaseUrl).buildUpon().path("/signin").clearQuery()
                    .appendQueryParameter("choose", "1").appendQueryParameter("next", uri.toString()).build()
            } else uri
            withContext(Dispatchers.Main) { CustomTabsIntent.Builder().build().launchUrl(activity, destination) }
        }
    }

    fun switchTo(userId: String) = scope.launch { action { readyOrThrow(); session.switchTo(userId) } }

    /** Removes local credentials only; native server revocation is not implemented yet. */
    fun signOut(userId: String) = scope.launch { action { readyOrThrow(); session.signOut(userId) } }

    internal fun callback(uri: String) = scope.launch {
        action { readyOrThrow(); session.completeLogin(uri, config.redirectUri) }
    }

    /** Install as a NETWORK interceptor so every redirect hop is host-checked. */
    fun authInterceptor(allowedHosts: Set<String>): Interceptor {
        require(allowedHosts.isNotEmpty())
        require(allowedHosts.all { it == it.lowercase() && '/' !in it && ':' !in it && it.isNotBlank() })
        return Interceptor { chain ->
            check(Looper.myLooper() != Looper.getMainLooper()) { "Authenticated network requests must run off the main thread" }
            require(chain.connection() != null) { "Use addNetworkInterceptor, not addInterceptor" }
            val request = chain.request()
            val builder = request.newBuilder().removeHeader("Authorization")
            if (request.url.isHttps && request.url.host in allowedHosts && request.url.port == 443) {
                val token = runBlocking(Dispatchers.IO) { readyOrThrow(); session.accessToken() }
                if (token != null) builder.header("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }
    }

    private suspend fun readyOrThrow() { checkNotNull(ready) { "Call GothwadAccounts.init from Application.onCreate" }.await() }
    private suspend fun action(block: suspend () -> Unit) {
        mutableError.value = null
        try { block() } catch (error: CancellationException) { throw error } catch (error: Exception) {
            mutableError.value = when (error) {
                is ActivityNotFoundException -> AuthFailure.BrowserUnavailable
                is LoginCancelledException -> AuthFailure.LoginCancelled
                is IllegalArgumentException -> AuthFailure.InvalidCallback
                is OAuthException -> if (error.rejected) AuthFailure.SessionExpired else AuthFailure.Unavailable
                is IOException -> AuthFailure.Unavailable
                else -> AuthFailure.StorageUnavailable
            }
        }
    }
}
