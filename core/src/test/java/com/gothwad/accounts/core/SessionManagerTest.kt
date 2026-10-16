package com.gothwad.accounts.core

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SessionManagerTest {
    private val account = GothwadAccount("user", "pawan")
    private val expired = Credential(account, "old-access", "old-refresh", 0)
    private class MemoryStore(var value: StoredSession) : TokenStore {
        var failWrites = false
        override fun read() = value
        override fun write(value: StoredSession) {
            if (failWrites) throw IOException("storage unavailable")
            this.value = value
        }
    }
    private open class Service : OAuthService {
        val calls = AtomicInteger()
        override fun exchange(code: String, verifier: String) = TokenResponse("access", "refresh", 3600, "Bearer")
        override fun refresh(token: String): TokenResponse { calls.incrementAndGet(); return TokenResponse("new-access", "new-refresh", 3600, "Bearer") }
        override fun userInfo(token: String) = GothwadAccount("user", "pawan")
    }
    private fun store() = MemoryStore(StoredSession(listOf(expired), "user"))

    @Test fun concurrentRefreshRotatesOnlyOnce() = runTest {
        val store = store()
        val service = Service()
        val manager = SessionManager(store, service) { 1000 }
        manager.load()
        val results = (1..20).map { async(Dispatchers.Default) { manager.accessToken() } }.awaitAll()
        assertTrue(results.all { it == "new-access" })
        assertEquals(1, service.calls.get())
        assertEquals("new-refresh", store.value.credentials.single().refreshToken)
    }
    @Test fun outageKeepsAccount() = runTest {
        val store = store()
        val service = object : Service() { override fun refresh(token: String): TokenResponse = throw IOException("offline") }
        val manager = SessionManager(store, service) { 1000 }
        manager.load()
        try { manager.accessToken(); fail("Expected outage") } catch (_: IOException) { }
        assertTrue(manager.state.value is AccountState.SignedIn)
        assertEquals("old-refresh", store.value.credentials.single().refreshToken)
    }
    @Test fun rejectedRefreshRemovesAccount() = runTest {
        val service = object : Service() { override fun refresh(token: String): TokenResponse = throw OAuthException(true) }
        val manager = SessionManager(store(), service) { 1000 }
        manager.load()
        try { manager.accessToken(); fail("Expected rejection") } catch (_: OAuthException) { }
        assertEquals(AccountState.SignedOut, manager.state.value)
    }
    @Test fun logoutDuringRefreshCannotResurrectAccount() = runTest {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val service = object : Service() {
            override fun refresh(token: String): TokenResponse {
                started.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                return super.refresh(token)
            }
        }
        val store = store()
        val manager = SessionManager(store, service) { 1000 }
        manager.load()
        val request = async(Dispatchers.Default) { manager.accessToken() }
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS))
            manager.signOut("user")
        } finally { release.countDown() }
        assertNull(request.await())
        assertTrue(store.value.credentials.isEmpty())
        assertEquals(AccountState.SignedOut, manager.state.value)
    }
    @Test fun pendingLoginSurvivesRestartAndIsSingleUse() = runTest {
        val store = MemoryStore(StoredSession())
        val first = SessionManager(store, Service()) { 1000 }
        first.load()
        val pending = first.beginLogin()
        val restored = SessionManager(store, Service()) { 1001 }
        restored.load()
        val redirect = "com.gothwad.accounts:/oauth/callback"
        val callback = "$redirect?state=${pending.state}&code=code"
        restored.completeLogin(callback, redirect)
        assertTrue(restored.state.value is AccountState.SignedIn)
        assertNull(store.value.pending)
        try { restored.completeLogin(callback, redirect); fail("Expected single-use callback") } catch (_: IllegalArgumentException) { }
    }
    @Test fun failedPersistenceDoesNotPublishSignOut() = runTest {
        val store = store()
        val manager = SessionManager(store, Service()) { 1000 }
        manager.load()
        store.failWrites = true
        try { manager.signOut("user"); fail("Expected storage failure") } catch (_: IOException) { }
        assertTrue(manager.state.value is AccountState.SignedIn)
    }
}
