package com.gothwad.accounts.core

import android.content.Context
import android.util.AtomicFile
import com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class Credential(val account: GothwadAccount, val accessToken: String, val refreshToken: String?, val expiresAt: Long)
@Serializable
internal data class PendingLogin(val state: String, val verifier: String, val createdAt: Long)
@Serializable
internal data class StoredSession(
    val credentials: List<Credential> = emptyList(),
    val activeUserId: String? = null,
    val pending: PendingLogin? = null,
)

internal interface TokenStore {
    fun read(): StoredSession
    fun write(value: StoredSession)
}

internal class EncryptedTokenStore(context: Context, config: GothwadConfig) : TokenStore {
    private val binding = "${context.packageName}|${config.clientId}|${config.authBaseUrl}|${config.redirectUri}".toByteArray()
    private val suffix = MessageDigest.getInstance("SHA-256").digest(binding).joinToString("") { "%02x".format(it) }
    private val alias = "gothwad.accounts.$suffix"
    private val file = AtomicFile(File(context.noBackupFilesDir, "gothwad-$suffix.enc"))
    private val aead by lazy {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val uri = "android-keystore://$alias"
        if (!keyStore.containsAlias(alias)) AndroidKeystoreKmsClient.generateNewAeadKey(uri)
        AndroidKeystoreKmsClient().getAead(uri)
    }
    private val json = Json { ignoreUnknownKeys = true }

    override fun read(): StoredSession {
        val encrypted = try { file.readFully() } catch (_: FileNotFoundException) { return StoredSession() }
        return json.decodeFromString(aead.decrypt(encrypted, binding).toString(Charsets.UTF_8))
    }

    override fun write(value: StoredSession) {
        val encrypted = aead.encrypt(json.encodeToString(value).toByteArray(), binding)
        val output = file.startWrite()
        try {
            output.write(encrypted)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}
