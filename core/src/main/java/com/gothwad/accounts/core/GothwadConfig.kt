package com.gothwad.accounts.core

import java.net.URI

enum class Scope(val wireValue: String) { Profile("profile"), Email("email"), OfflineAccess("offline_access") }
enum class BrokerPolicy { Disabled }

data class GothwadConfig(
    val clientId: String,
    val redirectUri: String,
    val scopes: Set<Scope> = setOf(Scope.Profile, Scope.Email, Scope.OfflineAccess),
    val authBaseUrl: String = "https://accounts.gothwadtech.com/api",
    val brokerPolicy: BrokerPolicy = BrokerPolicy.Disabled,
) {
    init {
        require(clientId.isNotBlank() && scopes.isNotEmpty())
        val base = URI(authBaseUrl)
        require(base.scheme == "https" && !base.host.isNullOrBlank())
        require(base.rawQuery == null && base.rawFragment == null && base.rawUserInfo == null)
        val redirect = URI(redirectUri)
        require(redirect.isAbsolute && !redirect.isOpaque && redirect.rawPath == "/oauth/callback")
        require(redirect.rawQuery == null && redirect.rawFragment == null && redirect.rawUserInfo == null)
        require(redirect.scheme == "https" || (redirect.scheme.contains('.') && redirect.rawAuthority == null))
        require(redirect.scheme != "https" || !redirect.host.isNullOrBlank())
    }
}
