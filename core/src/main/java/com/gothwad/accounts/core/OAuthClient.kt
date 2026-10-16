package com.gothwad.accounts.core

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

internal class OAuthException(val rejected: Boolean) : IOException("OAuth request failed")
@Serializable
internal data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("token_type") val tokenType: String,
)
@Serializable
private data class UserInfo(
    val sub: String,
    @SerialName("preferred_username") val username: String? = null,
    val name: String? = null,
    val email: String? = null,
    val picture: String? = null,
)
internal interface OAuthService {
    fun exchange(code: String, verifier: String): TokenResponse
    fun refresh(token: String): TokenResponse
    fun userInfo(token: String): GothwadAccount
}
internal class OAuthClient(private val config: GothwadConfig) : OAuthService {
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).callTimeout(30, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }
    private val base = config.authBaseUrl.trimEnd('/')

    override fun exchange(code: String, verifier: String): TokenResponse = token(
        "grant_type" to "authorization_code", "code" to code,
        "code_verifier" to verifier, "redirect_uri" to config.redirectUri,
    )
    override fun refresh(token: String): TokenResponse = token("grant_type" to "refresh_token", "refresh_token" to token)

    private fun token(vararg fields: Pair<String, String>): TokenResponse {
        val form = FormBody.Builder().add("client_id", config.clientId)
        fields.forEach { (key, value) -> form.add(key, value) }
        return json.decodeFromString<TokenResponse>(execute(Request.Builder().url("$base/oauth/token").post(form.build()).build())).also {
            require(it.tokenType.equals("Bearer", ignoreCase = true) && it.accessToken.isNotBlank())
            require(it.expiresIn in 1..86400)
        }
    }

    override fun userInfo(token: String): GothwadAccount {
        val info = json.decodeFromString<UserInfo>(execute(Request.Builder().url("$base/oauth/userinfo")
            .header("Authorization", "Bearer $token").build()))
        require(info.sub.isNotBlank())
        return GothwadAccount(info.sub, info.username, info.name, info.email, info.picture)
    }

    private fun execute(request: Request): String = client.newCall(request).execute().use { response ->
        val body = response.body?.string() ?: throw IOException("Empty OAuth response")
        if (!response.isSuccessful) {
            val error = runCatching { json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content }.getOrNull()
            throw OAuthException(response.code in 400..499 && error == "invalid_grant")
        }
        body
    }
}
