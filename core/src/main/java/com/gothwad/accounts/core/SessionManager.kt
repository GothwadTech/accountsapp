package com.gothwad.accounts.core

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SessionManager(
    private val store: TokenStore,
    private val oauth: OAuthService,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val refreshLocks = ConcurrentHashMap<String, Mutex>()
    private var data = StoredSession()
    private var generation = 0L
    private val mutableState = MutableStateFlow<AccountState>(AccountState.Initializing)
    val state = mutableState.asStateFlow()

    suspend fun load() = mutex.withLock { data = store.read(); publish() }

    suspend fun beginLogin(): PendingLogin = mutex.withLock {
        generation++
        val pending = PendingLogin(Pkce.random(), Pkce.random(), clock())
        save(data.copy(pending = pending))
        pending
    }

    suspend fun completeLogin(callback: String, redirect: String) {
        val (pending, params, epoch) = mutex.withLock {
            val pending = requireNotNull(data.pending)
            require(clock() - pending.createdAt in 0..600_000)
            val params = CallbackValidator.parameters(callback, redirect, pending.state)
            save(data.copy(pending = null))
            Triple(pending, params, generation)
        }
        if (params.containsKey("error")) throw LoginCancelledException()
        val tokens = oauth.exchange(params.getValue("code"), pending.verifier)
        val account = oauth.userInfo(tokens.accessToken)
        mutex.withLock {
            if (generation != epoch) throw IOException("Login superseded")
            val credential = Credential(account, tokens.accessToken, tokens.refreshToken, clock() + tokens.expiresIn * 1000)
            save(data.copy(credentials = data.credentials.filterNot { it.account.userId == account.userId } + credential, activeUserId = account.userId))
        }
    }

    suspend fun switchTo(userId: String) = mutex.withLock {
        require(data.credentials.any { it.account.userId == userId })
        save(data.copy(activeUserId = userId))
    }

    suspend fun signOut(userId: String) = mutex.withLock {
        generation++
        val remaining = data.credentials.filterNot { it.account.userId == userId }
        save(data.copy(credentials = remaining, activeUserId = if (data.activeUserId == userId) remaining.firstOrNull()?.account?.userId else data.activeUserId, pending = null))
    }

    suspend fun accessToken(): String? {
        val userId = mutex.withLock { data.activeUserId } ?: return null
        return refreshLocks.getOrPut(userId) { Mutex() }.withLock {
            val original = mutex.withLock { data.credentials.find { it.account.userId == userId } } ?: return@withLock null
            if (original.expiresAt - clock() > 60_000) return@withLock original.accessToken
            val tokens = try { oauth.refresh(original.refreshToken ?: throw OAuthException(true)) } catch (error: OAuthException) {
                if (error.rejected) mutex.withLock {
                    if (data.credentials.find { it.account.userId == userId } == original) {
                        val remaining = data.credentials.filterNot { it.account.userId == userId }
                        save(data.copy(credentials = remaining, activeUserId = if (data.activeUserId == userId) remaining.firstOrNull()?.account?.userId else data.activeUserId))
                    }
                }
                throw error
            }
            mutex.withLock persist@{
                if (data.credentials.find { it.account.userId == userId } != original) return@persist null
                val rotated = original.copy(accessToken = tokens.accessToken, refreshToken = tokens.refreshToken ?: original.refreshToken, expiresAt = clock() + tokens.expiresIn * 1000)
                save(data.copy(credentials = data.credentials.map { if (it.account.userId == userId) rotated else it }))
                if (data.activeUserId == userId) rotated.accessToken else null
            }
        }
    }

    private fun save(next: StoredSession) { store.write(next); data = next; publish() }
    private fun publish() {
        val all = data.credentials.map { it.account }
        val active = all.find { it.userId == data.activeUserId } ?: all.firstOrNull()
        mutableState.value = active?.let { AccountState.SignedIn(it, all) } ?: AccountState.SignedOut
    }
}
internal class LoginCancelledException : Exception()
