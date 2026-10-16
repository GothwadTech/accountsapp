package com.gothwad.accounts.core

import kotlinx.serialization.Serializable

@Serializable
data class GothwadAccount(
    val userId: String,
    val username: String? = null,
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
)

sealed interface AccountState {
    data object Initializing : AccountState
    data object SignedOut : AccountState
    data class SignedIn(val account: GothwadAccount, val all: List<GothwadAccount>) : AccountState
}

enum class AuthFailure { BrowserUnavailable, InvalidCallback, LoginCancelled, SessionExpired, Unavailable, StorageUnavailable }
